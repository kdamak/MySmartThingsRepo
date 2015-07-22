/**
 *  Device type just used for changing device icons to what ever you want.  Once changed, go back to using ST's default device type.
 */
metadata {
	definition (name: "My Device Icon Modifier", namespace: "jscgs350", author: "smarththings") {
		capability "Switch"
	}

	// tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true) {
			state "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
		}

		main "switch"
		details(["switch"])
	}
}
