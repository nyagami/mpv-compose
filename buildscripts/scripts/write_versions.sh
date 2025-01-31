# get versions from source code
MPV_VERSION=$(cat buildscripts/deps/mpv/_build$1/common/version.h | grep "#define VERSION" | cut -d '"' -f 2)
LIBPLACEBO_VERSION=$(cat buildscripts/deps/libplacebo/_build$1/src/version.h | grep "#define BUILD_VERSION" | cut -d '"' -f 2)
FFMPEG_VERSION=$(cat buildscripts/include/depinfo.sh | grep "v_ffmpeg=" | cut -d '=' -f 2)
# get build date from compiled object file
START_RODATA=0x$(readelf buildscripts/deps/mpv/_build$1/libmpv.so.p/common_version.c.o -S | grep .rodata | cut -d ' ' -f 27)
START=0x$(readelf buildscripts/deps/mpv/_build$1/libmpv.so.p/common_version.c.o -s | grep mpv_builddate | cut -d ' ' -f 7)
SIZE=$(readelf buildscripts/deps/mpv/_build$1/libmpv.so.p/common_version.c.o -s | grep mpv_builddate | cut -d ' ' -f 11)
SIZE=$(($SIZE - 1))
SKIP=$(($START_RODATA + $START))
dd if=buildscripts/deps/mpv/_build$1/libmpv.so.p/common_version.c.o of=date.txt bs=1 skip=$SKIP count=$SIZE
DATE=$(cat date.txt)
rm date.txt
# write versions to Utils.kt
sed -i "s/%MPV_VERSION%/$MPV_VERSION/g" app/src/main/java/is/xyz/mpv/Utils.kt
sed -i "s/%LIBPLACEBO_VERSION%/$LIBPLACEBO_VERSION/g" app/src/main/java/is/xyz/mpv/Utils.kt
sed -i "s/%FFMPEG_VERSION%/$FFMPEG_VERSION/g" app/src/main/java/is/xyz/mpv/Utils.kt
sed -i "s/%DATE%/$DATE/g" app/src/main/java/is/xyz/mpv/Utils.kt
