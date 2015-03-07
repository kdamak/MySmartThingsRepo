/*
 * Modeled after the ST device type to add the ALARM capability and def both() that gets called to close
 * the valve when a leak detector is tripped, or any other device that can trip alarms.
*/

metadata {
	// Automatically generated. Make future change here.
	definition (name: "My Zigbee Valve", namespace: "jscgs350", author: "SmartThings") {
		capability "Battery"
        capability "Alarm"
		capability "Configuration"
		capability "Refresh"
		capability "Switch"
		capability "Valve"

		fingerprint profileId: "0104", inClusters: "0000,0001,0003,0004,0005,0020,0006,0B02", outClusters: "0003"
	}

	// simulator metadata
	simulator {
		// status messages
		status "on": "on/off: 1"
		status "off": "on/off: 0"

		// reply messages
		reply "zcl on-off on": "on/off: 1"
		reply "zcl on-off off": "on/off: 0"
	}

	// UI tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", label: 'closed', action: "switch.on", icon: "st.Outdoor.outdoor16", backgroundColor: "#ffffff"
			state "on", label: 'open', action: "switch.off", icon: "st.Outdoor.outdoor16", backgroundColor: "#53a7c0"
		}
		main "switch"
		details(["switch"])
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	log.info description
	if (description?.startsWith("catchall:")) {
        def value = name == "switch" ? (description?.endsWith(" 1") ? "on" : "off") : null
		def result = createEvent(name: name, value: value)
        def msg = zigbee.parse(description)
		log.debug "Parse returned ${result?.descriptionText}"
		return result
		log.trace msg
		log.trace "data: $msg.data"
	}
	else {
		def name = description?.startsWith("on/off: ") ? "switch" : null
		def value = name == "switch" ? (description?.endsWith(" 1") ? "on" : "off") : null
		def result = createEvent(name: name, value: value)
		log.debug "Parse returned ${result?.descriptionText}"
		return result
	}
}

// Commands to device
def on() {
	log.debug "on()"
	sendEvent(name: "switch", value: "on")
	"st cmd 0x${device.deviceNetworkId} 1 6 1 {}"
}

def off() {
	log.debug "off()"
	sendEvent(name: "switch", value: "off")
	"st cmd 0x${device.deviceNetworkId} 1 6 0 {}"
}

def open() {
	log.debug "on()"
	sendEvent(name: "switch", value: "on")
	"st cmd 0x${device.deviceNetworkId} 1 6 1 {}"
}

def both() {
	log.debug "Closing valve due to an alarm condition!"
	sendEvent(name: "switch", value: "off")
	"st cmd 0x${device.deviceNetworkId} 1 6 0 {}"
}

def close() {
	log.debug "off()"
	sendEvent(name: "switch", value: "off")
	"st cmd 0x${device.deviceNetworkId} 1 6 0 {}"
}

def refresh() {
	log.debug "sending refresh command"
	"st rattr 0x${device.deviceNetworkId} 1 6 0"
}

def configure() {

	"zdo bind 0x${device.deviceNetworkId} 1 1 6 {${device.zigbeeId}} {}"
}
