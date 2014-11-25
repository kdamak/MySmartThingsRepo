/**
 *  MIMOLite Doorbell
 *
*/
metadata {
	definition (name: "MIMOLite Doorbell", namespace: "NTDCS", author: "Adam Newcomb") {
    	capability "Contact Sensor"	
	capability "Refresh"
        capability "Configuration"
        //uncomment for switch
        /*
        capability "Polling"
        capability "Switch"
        */
	}
	simulator {
		// reply messages
		reply "2001FF,delay 100,2502": "command: 2503, payload: FF"
		reply "200100,delay 100,2502": "command: 2503, payload: 00"

		// status messages
		status "open":  "command: 2001, payload: FF"
		status "closed": "command: 2001, payload: 00"

        //uncomment for switch
        /*
        status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"
        */
	}

	tiles {

        standardTile("contact", "device.contact", inactiveLabel: false) {
			state "open", label: "DingDong!", icon: "st.security.alarm.alarm", backgroundColor: "#ffa81e"
			state "closed", label: " ", icon: "st.security.alarm.clear", backgroundColor: "#79b821"
		}

        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

        standardTile("power", "device.power", canChangeIcon: true, canChangeBackground: true)
		{
        	state "off", label: 'OFF', backgroundColor: "#ffa81e", icon:"st.switches.light.off"
        	state "on", label: 'ON', backgroundColor: "#79b821", icon:"st.switches.light.on"
        }

		//uncomment for switch
        /*
		standardTile("switch", "device.switch", canChangeBackground: true, canChangeIcon: true, inactiveLabel: false) {
        	state "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
			state "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#ffa81e"
        }
        */

        valueTile("voltage", "device.voltage") {
			state("voltage", label:'${currentValue}v', unit:"ADC")
		}


		main (["contact"])

        //uncomment for switch
		details(["contact", "voltage", "refresh", "power" /* , "switch" */])
	}
}

def parse(String description) {

    def result = null
    def cmd = zwave.parse(description, [0x20: 1, 0x84: 1, 0x30: 1, 0x70: 1, 0x31: 3, 0x71: 1])
    if (cmd) {
        result = zwaveEvent(cmd)
        log.debug "parse cmd: '${cmd}' to result: '${result.inspect()}'"
    } else {
        log.debug "parse failed for event: '${description}'"
    }
    result
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd)
{
	log.debug "zwaveEvent SensorBinaryReport: '${cmd}'"
	sensorValueEvent(cmd.sensorValue)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd)
{
    //BUGBUG_NOTE: this is the event that gets sent from simluated messages and also if subscribed to association group 1
	log.debug "zwaveEvent BasicSet: '${cmd}'"
    sensorValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv3.SensorMultilevelReport cmd)
{
	log.debug "zwaveEvent SensorMultilevelReport: '${cmd}'"

    createEvent( name: "voltage", isStateChange:true, value:cmd.scaledSensorValue,descriptionText: "${device.displayName} measured voltage in ADC Counts")
}

def zwaveEvent(physicalgraph.zwave.commands.alarmv1.AlarmReport cmd)
{
	log.debug "zwaveEvent AlarmReport: '${cmd}'"

    switch (cmd.alarmType) {
        case 8:
            def map = [ name: "power", isStateChange:true]
            if (cmd.alarmLevel){
                map.value="off"
                map.descriptionText = "${device.displayName} lost power"
            }
            else {
                map.value="on"
                map.descriptionText = "${device.displayName} has power"
            }
            createEvent(map)
        break;
		default:
        	[:]
        break;
    }
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
	log.debug "zwaveEvent SwitchBinaryReport: '${cmd}'"
	switchValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	log.debug "zwaveEvent BasicReport: '${cmd}'"

    switchValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
	log.debug "zwaveEvent ConfigurationReport: '${cmd}'"
    [:]
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
    cmd.nodeId.each({log.debug "AssociationReport: '${cmd}', hub: '$zwaveHubNodeId' reports nodeId: '$it' is associated in group: '${cmd.groupingIdentifier}'"})
    [:]
}
def zwaveEvent(physicalgraph.zwave.Command cmd) {
	log.debug "zwaveEvent Command not Handled: '${cmd}'"
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

private sensorValueEvent(Short value) {

	def result = []

    if (value) {
    	log.debug "Open sensor value event: $value"
        result << createEvent(name: "contact", value: "open", descriptionText: "$device.displayName is open", isStateChange:true )
	} else {
        log.debug "Closed sensor value event: $value"
		result << createEvent(name: "contact", value: "closed", descriptionText: "$device.displayName is closed", isStateChange:true)
	}

    result
}

private switchValueEvent(Short value)
{
	createEvent([name: "switch", value: value ? "on" : "off", isStateChange:true ])
}


def refresh() {
	log.debug "executing 'refresh'"

    delayBetween([
	zwave.configurationV1.configurationGet(parameterNumber:3).format(),
	zwave.configurationV1.configurationGet(parameterNumber:4).format(),
	zwave.configurationV1.configurationGet(parameterNumber:5).format(),
	zwave.configurationV1.configurationGet(parameterNumber:6).format(),
	zwave.configurationV1.configurationGet(parameterNumber:7).format(),
        zwave.configurationV1.configurationGet(parameterNumber:8).format(),
        zwave.configurationV1.configurationGet(parameterNumber:9).format(),
        zwave.configurationV1.configurationGet(parameterNumber:11).format(),
        zwave.associationV1.associationGet(groupingIdentifier:1).format(),
        zwave.associationV1.associationGet(groupingIdentifier:2).format(),
        zwave.associationV1.associationGet(groupingIdentifier:3).format(),
        zwave.associationV1.associationGet(groupingIdentifier:4).format(),
        zwave.associationV1.associationGet(groupingIdentifier:5).format(),
        zwave.switchBinaryV1.switchBinaryGet().format(),
        zwave.alarmV1.alarmGet(alarmType:8).format(),
        zwave.sensorMultilevelV3.sensorMultilevelGet().format()
	],100)
}

// handle commands
def configure() {
	log.debug "executing 'configure'"

    /*
	8v=11000101 = 197
	7v=11000001 = 193

	4v=2741=10101011 = 171
	5v=2892=10110100 = 180

	.5v=631=00100111=39
	2v=2062=10000000=128

	24v=11101000 = 232
	23v=11100111 = 231

	1.5v=1687=01101001=105
 	1.25v=1433=01011001=89
  	1.125=1306=01010001=81
	1v=1179=01001001=73
    */
	def cmd = delayBetween([
    	//zwave.configurationV1.configurationSet(parameterNumber: 2, size: 1, configurationValue: [1]).format(),  // clear pulse meter counts
    	zwave.configurationV1.configurationSet(parameterNumber: 3, size: 1, configurationValue: [1]).format(),  // sig 1 triggers relay
        zwave.configurationV1.configurationSet(parameterNumber: 4, size: 1, configurationValue: [100]).format(),  // lower threshold, high
        zwave.configurationV1.configurationSet(parameterNumber: 5, size: 1, configurationValue: [39]).format(),  // lower threshold, low
        zwave.configurationV1.configurationSet(parameterNumber: 6, size: 1, configurationValue: [232]).format(),  // upper threshold, high
        zwave.configurationV1.configurationSet(parameterNumber: 7, size: 1, configurationValue: [231]).format(),  // upper threshold, low
        zwave.configurationV1.configurationSet(parameterNumber: 8, size: 1, configurationValue: [1]).format(),  // set to analog, below bounds
        zwave.configurationV1.configurationSet(parameterNumber: 9, size: 1, configurationValue: [255]).format(),  // disable periodic reports
        zwave.configurationV1.configurationSet(parameterNumber: 11, size: 1, configurationValue: [0]).format(),  // momentary relay
        zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId).format(),	//subscribe to basic sets on sig1
        zwave.associationV1.associationSet(groupingIdentifier:2, nodeId:zwaveHubNodeId).format(),	//subscribe to basic multisensorreports
        zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:zwaveHubNodeId).format(),	//subscribe to power alarm
        zwave.associationV1.associationRemove(groupingIdentifier:4, nodeId:zwaveHubNodeId).format(),	//unsubscribe from binary sensor reports
        zwave.associationV1.associationRemove(groupingIdentifier:5, nodeId:zwaveHubNodeId).format()	//unsubscribe from pulse meter events
	],100)
    //associationRemove
}

//uncomment for switch
/*
def poll() {
	// If you add the Polling capability to your device type, this command will be called approximately
	// every 5 minutes to check the device's state
	//zwave.basicV1.basicGet().format()
}

def on() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	],100)
}

def off() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	],100)
}
*/
