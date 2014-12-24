#include <string.h>

#include <sys/ioctl.h>
#include <sys/socket.h>

#include <linux/sockios.h>
#include <linux/if.h>
#include <linux/ethtool.h>

#include <jni.h>

jstring
Java_com_Chris_NetTool_Utils_wlan0DriverDesc(JNIEnv *env, jobject thiz)
{
    char buf[100];

    get_wlan0_driver_string(buf, sizeof(buf));

    return (*env)->NewStringUTF(env, buf);
}

void get_wlan0_driver_string(char *str, size_t str_size)
{
    int fd = socket(AF_INET, SOCK_DGRAM, 0);

    if (fd < 0)
    {
        snprintf(str, str_size, "socket failed");

        return;
    }

    struct ifreq ifr;

    memset(&ifr, 0, sizeof(ifr));

    strncpy(ifr.ifr_name, "wlan0", sizeof(ifr.ifr_name));

    struct ethtool_drvinfo edata;

    ifr.ifr_data = &edata;

    edata.cmd = ETHTOOL_GDRVINFO;

    if (ioctl(fd, SIOCETHTOOL, &ifr) < 0)
    {
        snprintf(str, str_size, "ioctl failed");

        return;
    }

    snprintf(str, str_size, "%s (%s)", edata.driver, edata.version);
}
