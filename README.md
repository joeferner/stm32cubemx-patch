This code fixes a bug in STM32CubeMX which only appears on non-Windows platforms as described [here](https://my.st.com/e8ee2d5b)

- Install STM32CubeMX as described [here](http://fivevolt.blogspot.com/2014/07/installing-stm32cubemx-on-linux.html)
- Create a backup of `plugins/ip/gpio.jar`
- Unpack `gpio.jar` into a new directory
- Run `java com.fernsroth.stm32cubmxpatch.Patch <gpio dir>`
- Repackage `gpio.jar`

![Screenshot](screenshot01.jpg)
