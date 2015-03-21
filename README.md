#Mi Band Notifier [![Build Status](https://travis-ci.org/vishnudevk/miband-notifier.svg)](https://travis-ci.org/vishnudevk/miband-notifier)

Warning: this app is very much in the development stages.

This app currently only works with the Xiaomi Mi Band, and requires the main app to be installed and paired with the band - as the band is not discoverable once paired.

The app currently only works with Lollipop due to significant incompatible changes in the Bluetooth stack, there are no immediate plans to change this.

I've only tested this on my Nexus 5, and I understand that some (older) BLE chips don't allow bonding, meaning this won't currently work.

Disclaimer: This app includes no warranty for your device, in my testing I've never bricked my band - but as I'm reverse engineering their API, it's entirely possible, albeit unlikely.



##Todo:

- New flag to not remove from queue if notification should be sent on reconnection with device
- Implement way to dynamically setup 'actions' to add to the queue for each app
    - For example, Vibrate for 100ms then flash for 50ms, then vibrate again for 200ms
    - Use conditions like Llama
- Option for only in 'Priority'/'None' modes.
- Better handling of no paired Mi Band, currently the app will simply do nothing.
- The band takes longer than the writes to adjust it's LE connection after setting the params, so they are effectively moot
- Tests

##Future:
- Pull in more data from the band
    - Maybe integrate with Google Fit?
