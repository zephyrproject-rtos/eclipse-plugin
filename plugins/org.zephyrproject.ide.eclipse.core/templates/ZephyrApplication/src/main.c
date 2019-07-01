#include <zephyr.h>
#include <misc/printk.h>

void main(void)
{
	printk("Hello World! %s\n", CONFIG_BOARD);
}
