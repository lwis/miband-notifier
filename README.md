#Mi Band Notifier [![Build Status](https://travis-ci.org/lwis/miband-notifier.svg?branch=master)](https://travis-ci.org/lwis/miband-notifier)

Warning: this app is very much in the development stages.

This app currently only works with the Xiaomi Mi Band, and requires the main app to be installed and paired with the band - as the band is not discoverable once paired.

The app currently only works with Lollipop due to significant incompatible changes in the Bluetooth stack, there are no immediate plans to change this.

##Todo:

- App icon
- Improve app list display
- Improve time pickers
- Improve Bluetooth connection reliability
- Better handling of no paired Mi Band
- Debug why the vibration duration can be quite inconsistent (requiring a band restart)
    - I think this is due to the poor Android Bluetooth stack
- Separate service for Bluetooth communications, using broadcasts to communicate between services/activities?
- Tests

##Future:

- Tasker plugin support
- Pull in more data from the band
    - Maybe integrate with Google Fit?