/*
 * Philio PSM01 3-in-1 Multi Sensor Device Type
 * based on SmartThings' Aeon Multi Sensor Reference Device Type
 */
 
 metadata {


	definition (name: "My PSM01 Sensor - Kitchen Refrigerator", namespace: "jscgs350", author: "SmartThings/Paul Spee") {
		capability "Contact Sensor"
		capability "Temperature Measurement"
		capability "Illuminance Measurement"
		capability "Configuration"
		capability "Sensor"
		capability "Battery"
        	capability "Refresh"
		capability "Polling"

		fingerprint deviceId: "0x2001", inClusters: "0x30,0x31,0x80,0x84,0x70,0x85,0x72,0x86"
	}

	tiles {

//		standardTile("contact", "device.contact", width: 2, height: 2) {
//            state "close", label:'closed', icon:"st.contact.contact.closed", backgroundColor:"#79b821"
//            state "open", label:'open', icon:"st.contact.contact.open", backgroundColor:"#ffa81e"
//        }
		standardTile("contact", "device.contact", width: 2, height: 2) {
			state "close", label: 'Frig Closed', icon: "st.fridge.fridge-closed", backgroundColor: "#ffffff"
			state "open", label: 'Frig Open', icon: "st.fridge.fridge-open", backgroundColor: "#ffa81e"
		}


		valueTile("temperature", "device.temperature", inactiveLabel: false) {
			state "temperature", label:'${currentValue}°',
			backgroundColors:[
				[value: 31, color: "#153591"],
				[value: 44, color: "#1e9cbb"],
				[value: 59, color: "#90d2a7"],
				[value: 74, color: "#44b621"],
				[value: 84, color: "#f1d801"],
				[value: 95, color: "#d04e00"],
				[value: 96, color: "#bc2323"]
			]
		}

		valueTile("illuminance", "device.illuminance", inactiveLabel: false) {
			state "luminosity", label:'${currentValue} ${unit}', unit:"lux"
		}
        
		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
			state "battery", label:'${currentValue}% battery', unit:""
		}
        
		standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}

		standardTile("refresh", "device.thermostatMode", inactiveLabel: false, decoration: "flat") {
			state "default", action:"polling.poll", icon:"st.secondary.refresh"
		}

		main(["contact", "temperature", "illuminance"])
		details(["contact", "temperature", "illuminance", "battery", "configure", "refresh"])
	}
}

preferences {
}

def installed() {
	log.debug "PSM01: Installed with settings: ${settings}"
	configure()
}

def updated() {
	log.debug "PSM01: Updated with settings: ${settings}"
    configure()

}

// parse() with a Map argument is called after a sendEvent(device)
// In this case, we are receiving an event from the PSM01 Helper App to generate a "inactive" event
def parse(Map evt){
	log.debug "Parse(Map) called with map ${evt}"
    def result = [];
    if (evt)
    	result << evt;
    log.debug "Parse(Map) returned ${result}"
    return result
}

// Parse incoming device messages to generate events
def parse(String description)
{
    log.debug "Parse called with ${description}"
	def result = []
	def cmd = zwave.parse(description, [0x20: 1, 0x31: 2, 0x30: 2, 0x80: 1, 0x84: 2, 0x85: 2])
	if (cmd) {
		if( cmd.CMD == "8407" ) { result << new physicalgraph.device.HubAction(zwave.wakeUpV1.wakeUpNoMoreInformation().format()) }
		def evt = zwaveEvent(cmd)
        result << createEvent(evt)
	}
	log.debug "Parse returned ${result}"
	return result
}

// Event Generation
def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd)
{
	[descriptionText: "${device.displayName} woke up", isStateChange: false]
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv2.SensorMultilevelReport cmd)
{
	def map = [:]
	switch (cmd.sensorType) {
		case 1:
			// temperature
			def cmdScale = cmd.scale == 1 ? "F" : "C"
			map.value = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmdScale, cmd.precision)
			map.unit = getTemperatureScale()
			map.name = "temperature"
			break;
		case 3:
			// luminance
			map.value = cmd.scaledSensorValue.toInteger().toString()
			map.unit = "lux"
			map.name = "illuminance"
			break;
	}
	map
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [:]
	map.name = "battery"
	map.value = cmd.batteryLevel > 0 ? cmd.batteryLevel.toString() : 1
	map.unit = "%"
	map.displayed = false
	map
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd) {
    log.debug "PSM01: SensorBinaryReport ${cmd.toString()}}"
    def map = [:]
    switch (cmd.sensorType) {
        case 10: // contact sensor
            map.name = "contact"
            if (cmd.sensorValue) {
                map.value = "open"
                map.descriptionText = "$device.displayName is open"
            } else {
                map.value = "close"
                map.descriptionText = "$device.displayName is closed"
            }
            break;
    }
    map
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	log.debug "PSM01: Catchall reached for cmd: ${cmd.toString()}}"
	[:]
}

def configure() {
    log.debug "PSM01: configure() called"
    
	delayBetween([
		
        //1 tick = 30 minutes
		zwave.configurationV1.configurationSet(parameterNumber: 10, size: 1, scaledConfigurationValue: 4).format(), // Auto report Battery time 1-127, default 12
		zwave.configurationV1.configurationSet(parameterNumber: 11, size: 1, scaledConfigurationValue: 2).format(), // Auto report Door/Window state time 1-127, default 12
		zwave.configurationV1.configurationSet(parameterNumber: 12, size: 1, scaledConfigurationValue: 2).format(), // Auto report Illumination time 1-127, default 12
        	zwave.configurationV1.configurationSet(parameterNumber: 13, size: 1, scaledConfigurationValue: 2).format(), // Auto report Temperature time 1-127, default 12
        	zwave.wakeUpV1.wakeUpIntervalSet(seconds: 1 * 3600, nodeid:zwaveHubNodeId).format(),						// Wake up every hour

    ])
}
