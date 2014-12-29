#Mi Band Notifier

This app currently only works with the Xiaomi Mi Band, and requires the main app to be installed and paired with the band - as the band is not discoverable once paired.

The app will alert the band with a short buzz, followed by a light indication when a notification is received.

The app currently only works with Lollipop due to incompatible significant changes in the Bluetooth stack, there are no immediate plans to change this.

##Todo:

- Improve Bluetooth connection reliability
- Handling of Bluetooth disabled/No paired Mi Band
- Restore original colour after notification
    - I think this is only achievable by setting the colour through a pref in the notify app
- Add apps to notify with settings from main app
    - incl. time period
- Debug why the vibration duration can be quite inconsistent (requiring a band restart)
    - I think this is due to the poor Android Bluetooth stack
- Separate service for Bluetooth communications, using broadcasts to communicate between services/activities?

##Future:

- Tasker plugin support
- Pull in more data from the band
    - Maybe integrate with Google Fit?