/**
 * MIMOlite device type for garage door button, including power failure indicator.  Be sure mimolite has jumper removed before
 * including the device to your hub, and tap Config to ensure power alarm is subscribed.
 *
 *  Author: Many ST community members
 *  Date: 2013-03-07,2014-02-03, 2014-03-07, 2015-01-04
 */
metadata {
	// Automatically generated. Make future change here.
	definition (name: "My MIMOlite - Garage Car Door v3", namespace: "jscgs350", author: "jscgs350") {
		capability "Alarm"
        capability "Momentary"
		capability "Polling"
        capability "Refresh"
        capability "Switch"
        capability "Contact Sensor"
        capability "Configuration"
        capability "Garage Door Control"
        attribute "power", "string"
        attribute "contactState", "string"
        attribute "powerState", "string"
	}

	// tile definitions        
	tiles(scale: 2) {
		multiAttributeTile(name:"contact", type: "lighting", width: 6, height: 4){
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
        standardTile("power", "device.power", width: 3, height: 2, inactiveLabel: false) {
        	state "dead", label: 'OFF', icon:"st.switches.switch.off", backgroundColor: "#ff0000"
        	state "alive", label: 'ON', icon:"st.switches.switch.on", backgroundColor: "#79b821"
        }
        standardTile("refresh", "device.switch", width: 3, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
		standardTile("configure", "device.configure", width: 3, height: 2, inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}
        valueTile("statusText", "statusText", inactiveLabel: false, width: 2, height: 2) {
			state "statusText", label:'${currentValue}', backgroundColor:"#ffffff"
		}
        main (["contact"])
        details(["contact", "refresh", "configure"])
    }
}

def parse(String description) {
    def result = null
    def cmd = zwave.parse(description, [0x72: 1, 0x86: 1, 0x71: 1, 0x30: 1, 0x31: 3, 0x35: 1, 0x70: 1, 0x85: 1, 0x25: 1, 0x03: 1, 0x20: 1, 0x84: 1])
	log.debug cmd
    if (cmd.CMD == "7105") {				//Mimo sent a power report lost power
        sendEvent(name: "power", value: "dead")
        sendEvent(name: "powerState", value: "NO POWER!")
    } else {
    	sendEvent(name: "power", value: "alive")
        sendEvent(name: "powerState", value: "electrical power.")
    }

	if (cmd) {
        result = createEvent(zwaveEvent(cmd))
    }
    
    def statusTextmsg = ""
    def timeString = new Date().format("h:mma MM-dd-yyyy", location.timeZone)
    statusTextmsg = "Garage door is ${device.currentState('contactState').value}.\nLast refreshed at "+timeString+"."
    sendEvent("name":"statusText", "value":statusTextmsg)
    
    return result
}

def sensorValueEvent(Short value) {
    if (value) {
    	log.debug "$device.displayName is now open"
		sendEvent(name: "contact", value: "open", descriptionText: "$device.displayName is open")
        sendEvent(name: "contactState", value: "OPEN (tap to close)")
    } else {
        log.debug "$device.displayName is now closed"
        sendEvent(name: "contact", value: "closed", descriptionText: "$device.displayName is closed")
        sendEvent(name: "contactState", value: "CLOSED (tap to open)")
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
	log.debug "zwaveEvent AlarmReport: '${cmd}'"

    switch (cmd.alarmType) {
        case 8:
            def map = [ name: "power", isStateChange:true]
            if (cmd.alarmLevel){
                map.value="dead"
                map.descriptionText = "${device.displayName} lost power"
                sendEvent(name: "powerState", value: "NO POWER!")
            }
            else {
                map.value="alive"
                map.descriptionText = "${device.displayName} has power"
                sendEvent(name: "powerState", value: "electrical power.")
            }
            sendEvent(map)
        break;
		default:
        	[:]
        break;
    }
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def push() {
	log.debug "Executing ACTUATE command for garage car door per user request"
	def cmds = [
		zwave.basicV1.basicSet(value: 0xFF).format(),
	]
}

def open() {
	log.debug "Executing OPEN command for garage car door per user request"
	def cmds = [
		zwave.basicV1.basicSet(value: 0xFF).format(),
	]
}

def close() {
	log.debug "Executing CLOSE command for garage car door per user request"
	def cmds = [
		zwave.basicV1.basicSet(value: 0xFF).format(),
	]
}

def poll() {
	log.debug "Executing Poll for garage car door"
/*	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.sensorBinaryV1.sensorBinaryGet().format(),
        zwave.basicV1.basicGet().format(),
		zwave.alarmV1.alarmGet().format() 
	],100)*/
}

def refresh() {
	log.debug "Executing Refresh for garage car door per user request"
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.sensorBinaryV1.sensorBinaryGet().format(),
        zwave.basicV1.basicGet().format(),
		zwave.alarmV1.alarmGet().format() 
	],100)
}

def configure() {
	log.debug "Executing Configure for garage car door per user request"
	def cmd = delayBetween([
		zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:[zwaveHubNodeId]).format(),
        zwave.configurationV1.configurationSet(configurationValue: [25], parameterNumber: 11, size: 1).format(),
	],100)
    log.debug "zwaveEvent ConfigurationReport: '${cmd}'"
}
