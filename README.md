#Mi Band Notifier [![Build Status](https://travis-ci.org/lwis/miband-notifier.svg?branch=master)](https://travis-ci.org/lwis/miband-notifier)

Warning: this app is very much in the development stages.

This app currently only works with the Xiaomi Mi Band, and requires the main app to be installed and paired with the band - as the band is not discoverable once paired.

The app currently only works with Lollipop due to significant incompatible changes in the Bluetooth stack, there are no immediate plans to change this.

Disclaimer: This app includes no warranty for your device, in my testing I've never bricked my band - but as I'm reverse engineering their API, it's entirely possible, albeit unlikely.



##Todo:

- Notification rate limiting, a queue may be appropriate here
    - New flag to not remove from queue if notification should be sent on reconnection with device
- Implement way to dynamically setup 'actions' to add to the queue for each app
    - For example, Vibrate for 100ms then flash for 50ms, then vibrate again for 200ms
- Option for only in 'Priority'/'None' modes.
- App icon
- Improve app list display
- Improve time pickers
- Improve Bluetooth failure reconnection reliability
- Better handling of no paired Mi Band
- The band takes longer than the writes to adjust it's LE connection after setting the params, so they are effectively moot
    - A change listener may be appropriate here
- Tests

##Future:

- Tasker plugin support
- Pull in more data from the band
    - Maybe integrate with Google Fit?