#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/init.h>
#include <linux/sched.h>
#include <linux/fdtable.h>
#include <linux/sched/signal.h>

static int process_id = 1; 

module_param(process_id, int, S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP);
MODULE_PARM_DESC(process_id, "Process ID to monitor (default: 1 for init)");

static void print_process_hierarchy(struct task_struct *task, int level)
{
    struct list_head *p;

    printk(KERN_INFO "%*s|- PID: %d, Name: %s\n", level * 4, "", task->pid, task->comm);

    list_for_each(p, &task->children) {
        struct task_struct *child_task = list_entry(p, struct task_struct, sibling);
        print_process_hierarchy(child_task, level + 1);
    }
}

static int __init monitor_init(void)
{
    struct task_struct *task;
    struct task_struct *target_task = NULL;

    if (process_id <= 0) {
        printk(KERN_INFO "Invalid process ID, defaulting to init process (PID: 1)\n");
        process_id = 1; 
    }

    printk(KERN_INFO "Monitoring process with PID: %d\n", process_id);

    for_each_process(task) {
        if (task->pid == process_id) {
            target_task = task;
            break;
        }
    }

    if (target_task == NULL) {
        printk(KERN_INFO "Process with PID %d not found\n", process_id);
        return -EINVAL;
    }

    print_process_hierarchy(target_task, 0);

    return 0;
}

static void __exit monitor_exit(void)
{
    printk(KERN_INFO "Monitoring module unloaded\n");
}

module_init(monitor_init);
module_exit(monitor_exit);

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Ankush S");
MODULE_DESCRIPTION("Kernel module to monitor processes by PID with hierarchical tree structure");
