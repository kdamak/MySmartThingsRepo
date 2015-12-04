metadata {
	// Automatically generated. Make future change here.
	definition (name: "My Aeon Metering Switch v2", namespace: "jscgs350", author: "SmartThings") {
		capability "Energy Meter"
		capability "Actuator"
		capability "Switch"
		capability "Power Meter"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
        capability "Configuration"
        capability "Sensor"

        attribute "power", "string"
        attribute "powerDisp", "string"
        attribute "powerOne", "string"
        attribute "powerTwo", "string"
        
        command "reset"
        command "configure"
        
		fingerprint inClusters: "0x25,0x32"
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
				attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
			}
            tileAttribute ("statusText", key: "SECONDARY_CONTROL") {
           		attributeState "statusText", label:'${currentValue}'       		
            }
		}

// Watts row

        valueTile("powerDisp", "device.powerDisp", width: 3, height: 2, inactiveLabel: false, decoration: "flat") {
            state ("default", icon: "st.secondary.activity", label:'Now ${currentValue}')
        }
        
        valueTile("powerOne", "device.powerOne", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state("default", label:'Low ${currentValue}')
        }
        
        valueTile("powerTwo", "device.powerTwo", width: 3, height: 2, inactiveLabel: false, decoration: "flat") {
            state("default", label:'High ${currentValue}')
        }
//

		valueTile("energy", "device.energy", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'${currentValue} kWh'
		}
        standardTile("reset", "device.energy", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'Reset', action:"reset", icon:"st.secondary.refresh-icon"
		}
		standardTile("configure", "device.power", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}
		standardTile("refresh", "device.power", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        valueTile("statusText", "statusText", inactiveLabel: false, width: 2, height: 2) {
			state "statusText", label:'${currentValue}', backgroundColor:"#ffffff"
		}
		main "powerDisp"
		details(["switch","refresh","reset","configure"])
	}
}

def parse(String description) {
	def result = null
	def cmd = zwave.parse(description, [0x20: 1, 0x32: 1])
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
        
    def statusTextmsg = ""
    statusTextmsg = "Switch is currently using ${device.currentState('powerDisp')?.value}, and hit a maximum of ${device.currentState('powerTwo')?.value}"
    sendEvent("name":"statusText", "value":statusTextmsg)
//    log.debug statusTextmsg

	return result
}

def zwaveEvent(physicalgraph.zwave.commands.meterv1.MeterReport cmd) {
    log.debug "zwaveEvent received ${cmd}"
    def dispValue
    def newValue
    def timeString = new Date().format("h:mma MM-dd-yyyy", location.timeZone)
	if (cmd.scale == 0) {
		[name: "energy", value: cmd.scaledMeterValue, unit: "kWh"]
	} else if (cmd.scale == 1) {
		[name: "energy", value: cmd.scaledMeterValue, unit: "kVAh"]
	}
	else {
            newValue = Math.round( cmd.scaledMeterValue )       // really not worth the hassle to show decimals for Watts
            if (newValue != state.powerValue) {
                dispValue = newValue+"w"
                sendEvent(name: "powerDisp", value: dispValue as String, unit: "")
                
                if (newValue < state.powerLow) {
                    dispValue = newValue+"w"+"on "+timeString
                    sendEvent(name: "powerOne", value: dispValue as String, unit: "")
                    state.powerLow = newValue
                }
                if (newValue > state.powerHigh) {
                    dispValue = newValue+"w "+"on "+timeString
                    sendEvent(name: "powerTwo", value: dispValue as String, unit: "")
                    state.powerHigh = newValue
                }
                state.powerValue = newValue
                [name: "power", value: newValue, unit: "W"]
            }
	}
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd)
{
	[
		name: "switch", value: cmd.value ? "on" : "off", type: "physical"
	]
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd)
{
	[
		name: "switch", value: cmd.value ? "on" : "off", type: "digital"
	]
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def on() {
	delayBetween([
			zwave.basicV1.basicSet(value: 0xFF).format(),
			zwave.switchBinaryV1.switchBinaryGet().format()
	])
}

def off() {
	delayBetween([
			zwave.basicV1.basicSet(value: 0x00).format(),
			zwave.switchBinaryV1.switchBinaryGet().format()
	])
}

def poll() {
    refresh()
}

def refresh() {
    log.debug "${device.name} refresh"
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.meterV2.meterGet(scale: 0).format(),
		zwave.meterV2.meterGet(scale: 2).format()
	])
}

def reset() {
    log.debug "${device.name} reset"

    state.powerHigh = 0
    state.powerLow = 99999

    sendEvent(name: "powerOne", value: "", unit: "")    
    sendEvent(name: "powerDisp", value: "", unit: "")    
    sendEvent(name: "powerTwo", value: "", unit: "")    

    def cmd = delayBetween( [
        zwave.meterV2.meterReset().format(),
        zwave.meterV2.meterGet(scale: 0).format()
    ])
    
    cmd
}

def configure() {
    log.debug "${device.name} configure"
	delayBetween([
    zwave.configurationV1.configurationSet(parameterNumber: 3, size: 1, scaledConfigurationValue: 1).format(),      // Disable selective reporting, so always update based on schedule below <set to 1 to reduce network traffic>
    zwave.configurationV1.configurationSet(parameterNumber: 4, size: 2, scaledConfigurationValue: 2).format(),     // (DISABLED by first option) Don't send unless watts have changed by 50 <default>
    zwave.configurationV1.configurationSet(parameterNumber: 8, size: 1, scaledConfigurationValue: 1).format(),     // (DISABLED by first option) Or by 10% <default>
    zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 4).format(),   // Combined energy in Watts
    zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: 15).format(),   // Every 15 Seconds (for Watts)
    zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 8).format(),    // Combined energy in kWh
    zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: 60).format(),  // every 60 seconds (for kWh)
    zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: 0).format(),    // Disable report 3
    zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: 0).format()   // Disable report 3
	])
}
