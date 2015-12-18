/**
 * MIMOlite device type for garage door button, including power failure indicator.  Be sure mimolite has jumper removed before
 * including the device to your hub, and tap Config to ensure power alarm is subscribed.
 *
 *  Author: Many ST community members
 */
metadata {
	// Automatically generated. Make future change here.
	definition (name: "My MIMOlite - Garage Car Door v3", namespace: "jscgs350", author: "jscgs350") {
        capability "Momentary"
		capability "Polling"
        capability "Refresh"
        capability "Switch"
        capability "Sensor"
        capability "Contact Sensor"
        capability "Configuration"
		capability "Actuator"
		capability "Door Control"
		capability "Garage Door Control"
        
        attribute "power", "string"
        attribute "contactState", "string"
        attribute "powerState", "string"
	}

	// tile definitions        
	tiles(scale: 2) {
		multiAttributeTile(name:"contact", type: "generic", width: 6, height: 4){
			tileAttribute ("device.contact", key: "PRIMARY_CONTROL") {
				attributeState "closed", label: 'Closed', action: "momentary.push", icon: "st.doors.garage.garage-closed", backgroundColor: "#79b821", nextState:"openingdoor"
				attributeState "open", label: 'Open', action: "momentary.push", icon: "st.doors.garage.garage-open", backgroundColor: "#ffa81e", nextState:"closingdoor"
                attributeState "closingdoor", label:'Closing', icon:"st.doors.garage.garage-closing", backgroundColor:"#ffd700"
                attributeState "openingdoor", label:'Opening', icon:"st.doors.garage.garage-opening", backgroundColor:"#ffd700"
			}
            tileAttribute ("statusText", key: "SECONDARY_CONTROL") {
           		attributeState "statusText", label:'${currentValue}'       		
            }
		}
        standardTile("switch", "device.switch", inactiveLabel: false) {
			state "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
		}        
        standardTile("power", "device.power", width: 2, height: 2, inactiveLabel: false) {
			state "powerOn", label: "Power On", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "powerOff", label: "Power Off", icon: "st.switches.switch.off", backgroundColor: "#ffa81e"
		}
        standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
		standardTile("configure", "device.configure", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}
        valueTile("statusText", "statusText", inactiveLabel: false, width: 2, height: 2) {
			state "statusText", label:'${currentValue}'
		}
        main (["contact", "switch"])
        details(["contact", "power", "refresh", "configure"])
    }
}

def parse(String description) {
    def result = null
    def cmd = zwave.parse(description, [0x72: 1, 0x86: 1, 0x71: 1, 0x30: 1, 0x31: 3, 0x35: 1, 0x70: 1, 0x85: 1, 0x25: 1, 0x03: 1, 0x20: 1, 0x84: 1])
    log.debug "command value is: $cmd.CMD"
    
    if (cmd.CMD == "7105") {				//Mimo sent a power loss report
    	log.debug "Device lost power"
    	sendEvent(name: "power", value: "powerOff", descriptionText: "$device.displayName lost power")
        sendEvent(name: "powerState", value: "powerOff")
    } else {
    	sendEvent(name: "power", value: "powerOn", descriptionText: "$device.displayName regained power")
        sendEvent(name: "powerState", value: "powerOn")
    }
    
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
    
	log.debug "Parse returned ${result?.descriptionText}"
    
    def statusTextmsg = ""
    def timeString = new Date().format("h:mma MM-dd-yyyy", location.timeZone)
    statusTextmsg = "Last refreshed at "+timeString+"."
    sendEvent("name":"statusText", "value":statusTextmsg)

    return result
}

def sensorValueEvent(Short value) {
    if (value) {
        sendEvent(name: "switch", value: "on")
		sendEvent(name: "contact", value: "open")
        sendEvent(name: "contactState", value: "OPEN (tap to close)")
    } else {
        sendEvent(name: "switch", value: "off")
        sendEvent(name: "contact", value: "closed")
        sendEvent(name: "contactState", value: "CLOSED (tap to open)")
    }
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
//	[name: "switch", value: cmd.value ? "on" : "off", type: "physical"]
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd)
{
	sensorValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
//	[name: "switch", value: cmd.value ? "on" : "off", type: "digital"]
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd)
{
	sensorValueEvent(cmd.sensorValue)
}

def zwaveEvent(physicalgraph.zwave.commands.alarmv1.AlarmReport cmd)
{
    log.debug "We lost power" //we caught this up in the parse method. This method not used.
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def on() {
	open()
}

def off() {
	close()
}

def open() {
	if (device.currentValue("contact") != "open") {
		log.debug "Sending ACTUATE event to open door"
		push()
	}
	else {
		log.debug "Not opening door since it is already open"
	}
}

def close() {
	if (device.currentValue("contact") != "closed") {
		log.debug "Sending ACTUATE event to close door"
		push()
	}
	else {
		log.debug "Not closing door since it is already closed"
	}
}

def push() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.sensorBinaryV1.sensorBinaryGet().format(),
        zwave.basicV1.basicGet().format(),
		zwave.alarmV1.alarmGet().format() 
	],100)
}

def poll() {
	log.debug "Executing Poll/Refresh for garage car door"
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.sensorBinaryV1.sensorBinaryGet().format(),
        zwave.basicV1.basicGet().format(),
		zwave.alarmV1.alarmGet().format() 
	],100)
}

def refresh() {
	poll()
}

def configure() {
	log.debug "Executing Configure for garage car door per user request" //setting up to monitor power alarm and actuator duration
	delayBetween([
		zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:[zwaveHubNodeId]).format(),
        zwave.configurationV1.configurationSet(configurationValue: [25], parameterNumber: 11, size: 1).format(),
        zwave.configurationV1.configurationGet(parameterNumber: 11).format()
	])
}
