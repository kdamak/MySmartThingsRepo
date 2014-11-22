/**
 *  SmartSense Virtual Momentary Contact Switch, Better Momentary for the MIMOlite
 *
 *  Capabilities to enable: Alarm, Contact Sensor, Momentary, Polling, Refresh, Switch
 *
 *  Author: SmartThings, jsconst@gmail.com
 *  Date: 2013-03-07,2014-02-03, 2014-03-07
 */
metadata {
	// Automatically generated. Make future change here.
	definition (name: "My MIMOlite - Garage Door", namespace: "jscgs350", author: "jsconst@gmail.com") {
		capability "Refresh"
		capability "Contact Sensor"
		capability "Momentary"
		capability "Polling"
		capability "Switch"

		attribute "alarmState", "string"
		attribute "alarmReport", "string"
	}

	// simulator metadata
	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"

		// reply messages
        reply "2001FF,delay 100,2502": "command: 2503, payload: FF"
		reply "200100,delay 100,2502": "command: 2503, payload: 00"
        
        // status messages
		status "open":  "command: 2001, payload: FF"
		status "closed": "command: 2001, payload: 00"
	}

	// tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true) {
			state "off", label: 'Push', action: "momentary.push", icon: "st.unknown.thing.thing-circle", backgroundColor: "#ffffff"
			state "on", label: 'Push', action: "switch.off", icon: "st.unknown.thing.thing-circle", backgroundColor: "#53a7c0"
		}
        standardTile("contact", "device.contact", inactiveLabel: false) {
			state "open", label: 'Garage\nCar Door\n${name}', icon: "st.contact.contact.open", backgroundColor: "#ffa81e"
			state "closed", label: 'Garage\nCar Door\n${name}', icon: "st.contact.contact.closed", backgroundColor: "#ffffff"
		}		
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        valueTile("alarm", "device.alarm", inactiveLabel: false, decoration: "flat") {
			state "alarm", label:'${currentValue}'
		}

		main (["switch", "contact", "alarm"])
		details(["switch", "contact", "refresh", "alarm"])
	}
}

def parse(String description) {
	def result = null
	def cmd = zwave.parse(description, [0x20: 1, 0x84: 1, 0x30: 1, 0x70: 1])
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
	log.debug "Parse returned ${result?.descriptionText}"
	return result
}

def sensorValueEvent(Short value) {
	if (value) {
		createEvent(name: "contact", value: "open", descriptionText: "$device.displayName is open")
	} else {
		createEvent(name: "contact", value: "closed", descriptionText: "$device.displayName is closed")
	}
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "physical"]
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd)
{
	sensorValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "digital"]
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd)
{
	sensorValueEvent(cmd.sensorValue)
}

def zwaveEvent(physicalgraph.zwave.commands.alarmv1.AlarmReport cmd)
{
	sensorValueEvent(cmd.sensorState)
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def push() {
	def cmds = [
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchBinaryV1.switchBinaryGet().format(),
		"delay 2000",
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	]
}

def on() {
	push()
}

def off() {
	[
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	]
}

def poll() {
	zwave.switchBinaryV1.switchBinaryGet().format()
}

def refresh() {
	zwave.switchBinaryV1.switchBinaryGet().format()
}
