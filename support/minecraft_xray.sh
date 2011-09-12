#!/bin/sh
LOGFILE=minecraft_xray_output_log.txt
cd "`dirname "$0"`"
echo >> ${LOGFILE}
echo "Launching Minecraft X-Ray..." >> ${LOGFILE}
java -Xms256m -Xmx1024m -cp AppleJavaExtensions.jar:jinput.jar:lwjgl.jar:lwjgl_test.jar:lwjgl_util.jar:lwjgl_util_applet.jar:lzma.jar:xray.jar:snakeyaml-1.9.jar -Djava.library.path=. com.apocalyptech.minecraft.xray.XRay 2>&1 | tee -a ${LOGFILE}

echo
echo "X-Ray log saved to ${LOGFILE}"
echo
