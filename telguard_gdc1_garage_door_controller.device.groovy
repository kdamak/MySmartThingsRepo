/**
 * MIMOlite device type for garage door button, including power failure indicator.  Be sure mimolite has jumper removed before
 * including the device to your hub, and tap Config to ensure power alarm is subscribed.
 *
 *  Author: Many ST community members
 */
metadata {
	// Automatically generated. Make future change here.
	definition (name: "My Telguard GDC1 Garage Door Controller v1", namespace: "jscgs350", author: "jscgs350") {
        capability "Momentary"
        capability "Relay Switch"
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
        
		command "on"
		command "off"

	}

	// UI tile definitions 
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
            	attributeState "doorClosed", label: "Closed", action: "on", icon: "st.doors.garage.garage-closed", backgroundColor: "#79b821", nextState:"openingdoor"
            	attributeState "doorOpen", label: "Open", action: "off", icon: "st.doors.garage.garage-open", backgroundColor: "#ffa81e", nextState:"closingdoor"
                attributeState "closingdoor", label:'Closing', icon:"st.doors.garage.garage-closing", backgroundColor:"#ffd700"
                attributeState "openingdoor", label:'Opening', icon:"st.doors.garage.garage-opening", backgroundColor:"#ffd700"
			}
            tileAttribute ("statusText", key: "SECONDARY_CONTROL") {
           		attributeState "statusText", label:'${currentValue}'       		
            }
		}
        standardTile("contact", "device.contact", inactiveLabel: false) {
			state "open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#ffa81e"
			state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
		}
        standardTile("refresh", "device.switch", width: 3, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("configure", "device.configure", width: 3, height: 2, inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}
        valueTile("statusText", "statusText", inactiveLabel: false, width: 2, height: 2) {
			state "statusText", label:'${currentValue}'
		}        
		main (["switch", "contact"])
		details(["switch", "refresh", "configure"])
    }
}

def parse(String description) {
	log.debug "description is: ${description}"

	def result = null
    def cmd = zwave.parse(description, [0x72: 1, 0x86: 1, 0x71: 1, 0x30: 1, 0x31: 3, 0x35: 1, 0x70: 1, 0x85: 1, 0x25: 1, 0x03: 1, 0x20: 1, 0x84: 1])
    
    log.debug "command value is: $cmd.CMD"
    
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
    
    def statusTextmsg = ""
    def timeString = new Date().format("h:mma MM-dd-yyyy", location.timeZone)
    statusTextmsg = "Last updated: "+timeString
    sendEvent("name":"statusText", "value":statusTextmsg)
    
	return result
}

def sensorValueEvent(Short value) {
	if (value) {
        sendEvent(name: "contact", value: "open")
        sendEvent(name: "switch", value: "doorOpen")
	} else {
        sendEvent(name: "contact", value: "closed")
        sendEvent(name: "switch", value: "doorClosed")
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
//	def doorState = device.currentValue('contact')
//    if ( doorState == "closed")
//		[name: "switch", value: cmd.value ? "on" : "doorOpening", type: "digital"]
//    else
//    	[name: "switch", value: cmd.value ? "on" : "doorClosing", type: "digital"]
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
	log.debug "Executing ACTUATE to open garage car door per user request"
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.sensorBinaryV1.sensorBinaryGet().format(),
        zwave.basicV1.basicGet().format()
	],100)
}

def off() {
	log.debug "Executing ACTUATE to close garage car door per user request"
	delayBetween([
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.sensorBinaryV1.sensorBinaryGet().format(),
        zwave.basicV1.basicGet().format()
	],100)
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
	log.debug "Executing ACTUATE for garage car door per user request"
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.sensorBinaryV1.sensorBinaryGet().format(),
        zwave.basicV1.basicGet().format()
	],100)
}

def poll() {
	refresh()
}

def refresh() {
	log.debug "Executing Refresh/Poll for garage car door per user request"
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.sensorBinaryV1.sensorBinaryGet().format(),
        zwave.basicV1.basicGet().format()
	],100)
}

def configure() {
	log.debug "Configuring...." //setting up to monitor power alarm and actuator duration
	delayBetween([
		zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:[zwaveHubNodeId]).format(),
        zwave.configurationV1.configurationSet(configurationValue: [25], parameterNumber: 11, size: 1).format(),
        zwave.configurationV1.configurationGet(parameterNumber: 11).format()
	])
}
