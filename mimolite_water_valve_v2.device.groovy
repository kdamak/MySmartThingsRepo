metadata {
    // Automatically generated. Make future change here.
    definition (name: "My MIMOlite - Main Water Valve v2", namespace: "jscgs350", author: "jsconst@gmail.com") {
		capability "Alarm"
		capability "Polling"
        capability "Refresh"
        capability "Switch"
		capability "Valve"
        capability "Contact Sensor"
        capability "Configuration"
        attribute "power", "string"
        attribute "valveState", "string"
        attribute "powerState", "string"
        
        fingerprint deviceId: "0x1000", inClusters: "0x72,0x86,0x71,0x30,0x31,0x35,0x70,0x85,0x25,0x03"
        
}

    // UI tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true, decoration: "flat"){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: 'Open Valve', action: "switch.on", icon: "st.valves.water.closed", backgroundColor: "#ff0000", nextState:"openingvalve"
				attributeState "off", label: 'Close Valve', action: "switch.off", icon: "st.valves.water.open", backgroundColor: "#53a7c0", nextState:"closingvalve"
				attributeState "closingvalve", label:'Closing', icon:"st.valves.water.closed", backgroundColor:"#ffd700"
				attributeState "openingvalve", label:'Opening', icon:"st.valves.water.open", backgroundColor:"#ffd700"
			}
            tileAttribute ("statusText", key: "SECONDARY_CONTROL") {
           		attributeState "statusText", label:'${currentValue}'       		
            }
        }
        standardTile("contact", "device.contact", width: 3, height: 2, inactiveLabel: false) {
            state "open", label: 'Open (On)', icon: "st.valves.water.open", backgroundColor: "#53a7c0"
            state "closed", label: 'Closed (Off)', icon: "st.valves.water.closed", backgroundColor: "#ff0000"
        }
        standardTile("power", "device.power", width: 3, height: 2, inactiveLabel: false) {
        	state "dead", label: 'OFF', backgroundColor: "#ff0000", icon:"st.switches.switch.off"
        	state "alive", label: 'ON', backgroundColor: "#79b821", icon:"st.switches.switch.on"
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
        main (["switch", "contact"])
        details(["switch", "refresh", "configure"])
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
    def timeString = new Date().format("h:mm a MM-dd-yyyy", location.timeZone)
    statusTextmsg = "Water valve is ${device.currentState('valveState').value}.\nLast activity was at "+timeString+".\nDevice has ${device.currentState('powerState').value}"
    sendEvent("name":"statusText", "value":statusTextmsg)
    log.debug statusTextmsg

    return result
}

def sensorValueEvent(Short value) {
    if (value) {
		log.debug "Main Water Valve is Open"
		sendEvent(name: "contact", value: "open", descriptionText: "$device.displayName is open")
        sendEvent(name: "valveState", value: "flowing water (tap to close)")
    } else {
    	log.debug "Main Water Valve is Closed"
        sendEvent(name: "contact", value: "closed", descriptionText: "$device.displayName is closed")
        sendEvent(name: "valveState", value: "NOT flowing water (tap to open)")
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

def off() {
	log.debug "Closing Main Water Valve per user request"
	delayBetween([
        zwave.basicV1.basicSet(value: 0xFF).format(),
        zwave.switchBinaryV1.switchBinaryGet().format()
    ])
}

def both() {
	log.debug "Closing Main Water Valve due to an alarm condition"
	delayBetween([
        zwave.basicV1.basicSet(value: 0xFF).format(),
        zwave.switchBinaryV1.switchBinaryGet().format()
    ])
}

def on() {
	log.debug "Opening Main Water Valve per user request"
	delayBetween([
        zwave.basicV1.basicSet(value: 0x00).format(),
        zwave.switchBinaryV1.switchBinaryGet().format()
    ])
}

def poll() {
	log.debug "Executing Poll for Main Water Valve"
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.sensorBinaryV1.sensorBinaryGet().format(),
        zwave.basicV1.basicGet().format(),
		zwave.alarmV1.alarmGet().format() 
	],100)
}

def refresh() {
	log.debug "Executing Refresh for Main Water Valve per user request"
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.sensorBinaryV1.sensorBinaryGet().format(),
        zwave.basicV1.basicGet().format(),
		zwave.alarmV1.alarmGet().format() 
	],100)
}


def configure() {
	log.debug "Executing Configure for Main Water Valve per user request"
	def cmd = delayBetween([
		zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:[zwaveHubNodeId]).format(), //subscribe to power alarm
        zwave.configurationV1.configurationSet(configurationValue: [25], parameterNumber: 11, size: 1).format(),
        zwave.configurationV1.configurationSet(parameterNumber: 11, size: 1, configurationValue: [0]).format(), // momentary relay disable=0 (default)
	],100)
    log.debug "zwaveEvent ConfigurationReport: '${cmd}'"
}
