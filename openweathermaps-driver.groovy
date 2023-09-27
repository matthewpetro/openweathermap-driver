// Use OpenWeather's API to get weather data and make it available to apps in Hubitat.
// OpenWeather's API documentation can be found at https://openweathermap.org/api/one-call-3

import groovy.transform.Field

metadata {
    definition(name: 'OpenWeather driver', namespace: 'MPetro', author: 'Matthew Petro') {
        capability 'Refresh'

        attribute 'currentWeatherId', 'number'
        attribute 'currentWeather', 'string'
        attribute 'currentWindSpeed', 'number'
        attribute 'probabilityOfPrecipitation', 'number'
    }

    preferences {
        input name: 'apiKey', type: 'text', title: 'OpenWeatherMap API key', defaultValue: null
        input name: 'pollInterval', type: 'number', title: 'API polling interval in minutes. 0 means no automatic polling.', defaultValue: 60, range: '0..1440'
        input name: 'txtEnable', type: 'bool', title: 'Enable descriptionText logging', defaultValue: true
    }
}

def installed() {
    if (txtEnable) log.debug "${device.displayName} installed"
    initialize()
}

def uninstalled() {
    if (txtEnable) log.debug "${device.displayName} uninstalled"
    unschedule('refresh')
}

def updated() {
    if (txtEnable) log.debug "${device.displayName} updated"
    initialize()
}

def initialize() {
    refresh()
}

def refresh() {
    if (txtEnable) log.info "${device.displayName} polling for weather data"
    asynchttpGet('handleApiResponse', [uri: getApiUrl(), timeout: 10])
    if (pollInterval > 0) {
        runIn(pollInterval * 60, 'refresh', [overwrite: true])
    }
}

def handleApiResponse(response, data) {
    if (!response.error) {
        if (txtEnable) log.info "${device.displayName}, received updated weather data from API"

        def descriptionText

        def currentWeatherId = response.json.current.weather[0].id
        descriptionText = "${device.displayName}, setting current weather ID to ${currentWeatherId}"
        if (txtEnable) log.info descriptionText
        sendEvent(name: 'currentWeatherId', value: currentWeatherId, descriptionText: descriptionText)

        def currentWeather = response.json.current.weather[0].main
        descriptionText = "${device.displayName}, setting current weather to ${currentWeather}"
        if (txtEnable) log.info descriptionText
        sendEvent(name: 'currentWeather', value: currentWeather, descriptionText: descriptionText)

        def currentWindSpeed = response.json.current.wind_speed
        descriptionText = "${device.displayName}, setting current wind speed to ${currentWindSpeed}"
        if (txtEnable) log.info descriptionText
        sendEvent(name: 'currentWindSpeed', value: currentWindSpeed, descriptionText: descriptionText)

        def probabilityOfPrecipitation = response.json.hourly[0].pop
        descriptionText = "${device.displayName}, setting probability of precipitation to ${probabilityOfPrecipitation}"
        if (txtEnable) log.info descriptionText
        sendEvent(name: 'probabilityOfPrecipitation', value: probabilityOfPrecipitation, descriptionText: descriptionText)
    } else {
        log.error "Error updating weather data: ${response.status} ${response.errorMessage}"
    }
}

def getApiUrl() {
    def latitude = location.latitude.toString()
    def longitude = location.longitude.toString()
    return "https://api.openweathermap.org/data/3.0/onecall?lat=${latitude}&lon=${longitude}&exclude=minutely,daily&units=imperial&appid=${apiKey}"
}
