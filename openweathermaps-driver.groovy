import groovy.transform.Field

@Field static final Map WEATHER_CONDITIONS = [
    thunderstorm: 'thunderstorm',
    drizzle: 'drizzle',
    rain: 'rain',
    dust: 'dust',
    sand: 'sand',
    good: 'good'
]

metadata {
    definition(name: 'Open Weather Maps driver', namespace: 'Petro', author: 'Matthew Petro') {
        capability 'Temperature Measurement'
        capability 'Pressure Measurement'
        capability 'Refresh'

        attribute 'currentWeatherCondition', 'enum',
            [
                WEATHER_CONDITIONS.thunderstorm,
                WEATHER_CONDITIONS.drizzle,
                WEATHER_CONDITIONS.rain,
                WEATHER_CONDITIONS.dust,
                WEATHER_CONDITIONS.sand,
                WEATHER_CONDITIONS.good
            ]
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

def mapWeatherIdToString(id) {
    if (id >= 200 && id <= 299) {
        return WEATHER_CONDITIONS.thunderstorm
    }
    if (id >= 300 && id <= 399) {
        return WEATHER_CONDITIONS.drizzle
    }
    if (id >= 500 && id <= 599) {
        return WEATHER_CONDITIONS.rain
    }
    if (id == 731 || id == 761) {
        return WEATHER_CONDITIONS.dust
    }
    if (id == 751) {
        return WEATHER_CONDITIONS.sand
    }
    return WEATHER_CONDITIONS.good
}

def handleApiResponse(response, data) {
    if (!response.error) {
        if (txtEnable) log.info "${device.displayName}, received updated weather data from API"
        def weather = mapWeatherIdToString(response.json.current.weather[0].id)
        def descriptionText = "${device.displayName}, setting current weather condition to ${weather}"
        if (txtEnable) log.info descriptionText
        sendEvent(name: 'currentWeatherCondition', value: weather, descriptionText: descriptionText)
    } else {
        log.error "Error updating weather data: ${response.status} ${response.errorMessage}"
    }
}

def getApiUrl() {
    def latitude = location.latitude.toString()
    def longitude = location.longitude.toString()
    return "https://api.openweathermap.org/data/3.0/onecall?lat=${latitude}&lon=${longitude}&exclude=minutely,daily&units=imperial&appid=${apiKey}"
}