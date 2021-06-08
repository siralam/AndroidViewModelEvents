# Handling Events in ViewModel

The idea and content of this article are actually just a summary of the below 2 articles:

[The SingleLiveData use case](https://medium.com/androiddevelopers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150)  
[Android SingleLiveEvent Redux with Kotlin Flow](https://proandroiddev.com/android-singleliveevent-redux-with-kotlin-flow-b755c70bb055)

## Background

When we use MVVM in our project, we have to bear in mind ViewModel is not only a representation of View states, but also emitter of events. 

### What is the difference between an Event and a View state?

View state is a **state**, it persists through time and interactions until it changes;  
While events should be **consumed**, happens once and only once.

Example of events:
1. Notifications
2. Toast messages
3. Dialog messages
4. Navigations
5. Server communications
6. Bluetooth signals
7. Web socket event

etc...

For simplicity, the below sections will use Dialog message as an example.

## Why can't I use `LiveData<String?>`?

This is the most straight forward solution when everyone first uses MVVM.  `null` indicates that we don't have to display a dialog, while a non-null `String`, which is essentially the dialog message, when emitted, will be observed by `View` and displays a dialog.

### Viewodel
```kotlin
    val apiResponse = MutableLiveData<String?>(null)

    fun callSomeApi() {
        viewModelScope.launch {
            delay(3000)
            apiResponse.value = "I am some response"
        }
    }
```

### View
```kotlin
        vm.apiResponse.observe(viewLifecycleOwner) {
            if (!it.isNullOrBlank()) {
                showDialog(it)
            }
        }
```

The problem here is that dialog message is not consumed. For whatever reason if `apiResponse` is observed again, the dialog message will be displayed again.

A quick demo can be done by rotating the device. Even if you dismissed the dialog, if you rotate your device again, the dialog will be displayed again.

You may say, hey, my app is always portrait, why do I need to handle device rotation?

The truth is, this does not only happen in device rotation. Your fragment views can be destroyed in various scenarios, such as OS memory management, user changed permission, ViewPager destroyed it, etc. And you should also consider the possibility that this ViewModel may be shared to other Views in your future development.

## The LiveData<Event<String>> solution

In the article [The SingleLiveData use case](https://medium.com/androiddevelopers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150) Jose suggested to use `LiveData<Event>`. The idea here is to wrap your data in an `Event` class, which its content is **consumable**. 

### ViewModel
```kotlin
    val apiResponse = MutableLiveData<Event<String>>()

    fun callSomeApi() {
        viewModelScope.launch {
            delay(3000)
            apiResponse.value = Event("I am some response")
        }
    }
```

### View
```kotlin
        vm.apiResponse.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                showDialog(it)
            }
        }
```

And you can also create an extension for easier usage:
```kotlin
fun <T> LiveData<Event<T>>.observeEvent(owner: LifecycleOwner, eventHandler: (T) -> Unit) {
    this.observe(owner) {
        it.getContentIfNotHandled()?.let { content ->
            eventHandler(content)
        }
    }
}
```

```kotlin
        vm.apiResponse.observeEvent(viewLifecycleOwner) {
            showDialog(it)
        }
```

This method is already close to perfect. The only case it cannot handle is "Storing multiple unhandled events".

For example, if you app is listening to bluetooth signals, which for every interested detection, you will add an item to your RecyclerView. You don't want to lose any detection when the app is in the background (User pressed Home button). If you use `LiveData<Event>` for this case, when user comes back from the background, he can only gets the latest emission, even if your ViewModel actually received 100 signals in the background.

So it really depends on your use-case. If you want the latest event to override previous ones, `LiveData<Event>` is good to go. If not, you will need the below one.

## Kotlin coroutine `Channel`

What is a channel? According to [Android SingleLiveEvent Redux with Kotlin Flow](https://proandroiddev.com/android-singleliveevent-redux-with-kotlin-flow-b755c70bb055):

```
… channels also have their application use-cases. Channels are used to handle events that must be processed exactly once. This happens in a design with a type of event that usually has a single subscriber, but intermittently (at startup or during some kind of reconfiguration) there are no subscribers at all, and there is a requirement that all posted events must be retained until a subscriber appears.
```

This sounds like a perfect fit to our use case!

And it is also very simple to use.

### ViewModel
```kotlin
    val apiResponse = Channel<String>(Channel.BUFFERED)

    fun callSomeApi() {
        viewModelScope.launch {
            delay(3000)
            apiResponse.send("I am some response")
        }
    }
```

### View
```kotlin
        vm.apiResponse.receiveAsFlow().onEach {
            showDialog(it)
        }.launchIn(viewLifecycleOwner.lifecycleScope)
```

## Wait! You have not done yet!

What's the problem remaining? It's lifecycle.

When you use `LiveData` and `observe(viewLifeCyclerOwner) { }`, Android lifecycle library handled lifecycle for you.

From the documentation of `observe()`:
```
...
The observer will only receive events if the owner is in STARTED or RESUMED state (active).
...
```

And if you use `Channel`, you will have to handle this by yourself! Otherwise your app will crash if you try to commit a fragment transaction when your app is in PAUSED state.

### View
```kotlin
    private var job: Job? = null

    override fun onStart() {
        super.onStart()
        job = vm.apiResponse.receiveAsFlow().onEach {
            showDialog(it)
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onStop() {
        super.onStop()
        job?.cancel()
    }
```

## OMG... This is so troublesome!

Yes, I agree. But we have the `launchWhenResumed { }` extension to use:

```kotlin
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            vm.apiResponse.receiveAsFlow().collect {
                showDialog(it)
            }
        }
```

Now, if you have already read the article [Android SingleLiveEvent Redux with Kotlin Flow](https://proandroiddev.com/android-singleliveevent-redux-with-kotlin-flow-b755c70bb055), you may notice that the author is discouraging the use of `launchWhenXXXX`. But I think the reason is way to subtle to introduce a custom observer just for that scenario.

What he is saying is basically, if you use `launchWhenResumed`, events will be dropped between RESUMED state and DESTROYED state because the job will be cancelled in DESTROYED state instead of PAUSED state. But the time between these 2 events are basically negligible, and when I test configuration changes I don't encounter any loss of event at all. To be honest I doubt you can only observe this behaviour only when you explicitly log the STOP event. So I think introducing a new observer is over-engineering.

## TDLR;

If you only want the latest event, use `LiveData<Event>`.  
If you don't want to lose any event, use `Channel`.

## Bonus: Replace LiveData with StateFlow

Actually Google suggests us to use `Flow` instead of `LiveData` if you are developing a Kotlin project. It is actually easy to migrate.

