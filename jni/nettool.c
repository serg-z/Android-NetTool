#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <stddef.h>

#define __STDC_FORMAT_MACROS
#include <inttypes.h>

#include <sys/ioctl.h>
#include <sys/socket.h>

#include <linux/sockios.h>
#include <linux/if.h>
#include <linux/ethtool.h>

#include <jni.h>

jstring
Java_com_Chris_NetTool_Utils_wlan0DriverDesc(JNIEnv *env, jobject thiz);

jstring
Java_com_Chris_NetTool_Utils_dumpStats(JNIEnv *env, jobject thiz);

void get_wlan0_driver_string(char *str, size_t str_size);
void dump_stats(char *str, size_t str_size);

jstring
Java_com_Chris_NetTool_Utils_wlan0DriverDesc(JNIEnv *env, jobject thiz)
{
    char buf[100];

    get_wlan0_driver_string(buf, sizeof(buf));

    return (*env)->NewStringUTF(env, buf);
}

jstring
Java_com_Chris_NetTool_Utils_dumpStats(JNIEnv *env, jobject thiz)
{
    // 20 lines with 256 chars

    char buf[20 * 256 + 1];

    dump_stats(buf, sizeof(buf));

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

void dump_stats(char *str, size_t str_size)
{
    const char *inter = "wlan0";

    int fd = socket(AF_INET, SOCK_DGRAM, 0);

    if (fd < 0)
    {
        snprintf(str, str_size, "socket failed");

        return;
    }

    // stats count

    struct ethtool_drvinfo drvinfo;

    drvinfo.cmd = ETHTOOL_GDRVINFO;

    struct ifreq ifr;

    memset(&ifr, 0, sizeof(ifr));

    strncpy(ifr.ifr_name, inter, sizeof(ifr.ifr_name));

    ifr.ifr_data = &drvinfo;

    if (ioctl(fd, SIOCETHTOOL, &ifr) < 0)
    {
        snprintf(str, str_size, "stats count ioctl failed");

        return;
    }

    if (drvinfo.n_stats < 1)
    {
        snprintf(str, str_size, "no stats available");

        return;
    }

    // strings

    int len = *(uint32_t*)((char*)&drvinfo + offsetof(struct ethtool_drvinfo, n_stats));

    struct ethtool_gstrings *strings;

    strings = calloc(1, sizeof(*strings) + len * ETH_GSTRING_LEN);

    if (!strings)
    {
        snprintf(str, str_size, "strings allocation failed");

        return;
    }

    strings->cmd = ETHTOOL_GSTRINGS;
    strings->string_set = ETH_SS_STATS;
    strings->len = len;

    memset(&ifr, 0, sizeof(ifr));

    strncpy(ifr.ifr_name, inter, sizeof(ifr.ifr_name));

    ifr.ifr_data = strings;

    if (ioctl(fd, SIOCETHTOOL, &ifr) < 0)
    {
        snprintf(str, str_size, "strings ioctl failed");

        return;
    }

    // stats

    struct ethtool_stats *stats;

    stats = calloc(1, drvinfo.n_stats * sizeof(uint64_t) + sizeof(struct ethtool_stats));

    if (!stats)
    {
        snprintf(str, str_size, "stats allocation failed");

        return;
    }

    stats->cmd = ETHTOOL_GSTATS;
    stats->n_stats = drvinfo.n_stats;

    memset(&ifr, 0, sizeof(ifr));

    strncpy(ifr.ifr_name, inter, sizeof(ifr.ifr_name));

    ifr.ifr_data = stats;

    if (ioctl(fd, SIOCETHTOOL, &ifr) < 0)
    {
        snprintf(str, str_size, "stats ioctl failed");

        return;
    }

    // print to string

    memset(str, 0, str_size);

    int i;

    char buf[256];

    for (i = 0; i < drvinfo.n_stats; ++i)
    {
        snprintf(buf, sizeof(buf), "%s - %" PRIu64 "\n", &strings->data[i * ETH_GSTRING_LEN],
            ((uint64_t)stats->data[i]));

        strcat(str, buf);
    }

    free(strings);
    free(stats);
}
