#include <zephyr.h>
#include <sys/printk.h>

void main(void)
{
	printk("Hello World! %s\n", CONFIG_BOARD);
}
