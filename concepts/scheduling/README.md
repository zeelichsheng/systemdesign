# Scheduling

Scheduling is the method by which work specified by some means is assigned to resources that complete the work. A scheduler is what carries out the scheduling activity.

A scheduler may aim at one of many goals, for example, maximizing throughput (the total amount of work completed per time unit), minimizing response time (time from work becoming enabled until the first point it begins execution on resources), or minimizing latency (the time between work becoming enabled and its subsequent completion), maximizing fairness (equal CPU time to each process, or more generally appropriate times according to the priority and workload of each process). In practice, these goals often conflict (e.g. throughput versus latency), thus a scheduler will implement a suitable compromise.

## Scheduling Algorithm

[OS Dev](http://wiki.osdev.org/Scheduling_Algorithms)

[Wikipedia](https://en.wikipedia.org/wiki/Scheduling_(computing))

A scheduling algorithm is used for distributing resources among parties which simultaneously and asynchronously request them. Scheduling disciplines are used in routers (to handle packet traffic) as well as in operating systems (to share CPU time among both threads and processes), disk drives (I/O scheduling), printers (print spooler), most embedded systems, etc.

The goal of any scheduling algorithm is to fulfill a number of criteria:

    1. No task must be starved of resources - all tasks must get their chance at CPU time.
    2. If using priorities, a low-priority task must not hold up a high-priority task.
    3. The scheduler must scale well with a growing number of tasks, ideally being O(1).
    
### Interactive Scheduling Algorithms

#### Round Robin

Round Robin is the simplest algorithm for a preemptive scheduler. Only a single queue of processes is used. When the system timer fires, the next process in the queue is switched to, and the preempted process is put back into the queue.

Each process is assigned a time slice or "quantum". This quantum dictates the number of system timer ticks the process may run for before being preempted. For example, if the timer runs at 100Hz, and a process' quantum is 10 ticks, it may run for 100 milliseconds (10/100 of a second). To achieve this, the running process is given a variable that starts at its quantum, and is then decremented each tick until it reaches zero. The process may also relinquish its quantum by doing a blocking system call (i.e. I/O), like in other preemptive algorithms.

In the Round Robin algorithm, each process is given an equal quantum; the big question is how to choose the time quantum.

Here are some considerations: The smaller the quantum, the larger the proportion of the time used in context switches. Interactive processes should do I/O before being preempted, so that unnecessary preemptions are avoided.
The larger the quantum, the "laggier" the user experience - quanta above 100ms should be avoided. A frequently chosen compromise for the quantum is between 20ms and 50ms.

Advantages of Round Robin include its simplicity and strict "first come, first served" nature. Disadvantages include the absence of a priority system: lots of low privilege processes may starve one high privilege one.

#### Priority-Based Round Robin

Priority scheduling is similar to Round Robin, but allows a hierarchy of processes. Multiple process queues are used, one for each priority. As long as there are processes in a higher priority queue, they are run first. For example, if you have 2 queues, "high" and "low", in this state:

"high": X

"low": xterm, vim, firefox

The first process to run would be X, then if it blocked (for I/O, probably), the state would be:

"high":

"low": xterm, vim, firefox

The next process that would run would be xterm. If process "kswapd" is added to "high", it would then get the next quantum:

"high": kswapd

"low": vim, firefox, xterm

There are usually between four and sixteen queues used in a priority scheduler. Advantages of this algorithm are simplicity and reasonable support for priorities. The disadvantage (or possible advantage) is that privileged processes may completely starve unprivileged ones. This is less of a problem than it appears, because processes (especially daemons, which are usually privileged) are usually blocked for I/O.

### Batch Scheduling Algorithms
    
#### First Come First Served

This scheduling method is used on Batch-Systems, it is NON-PREEMPTIVE. It implements just one queue which holds the tasks in order they come in.

The order the tasks arrive is very important for the Turnaround-Time:

Task1(24) Task2(6) Task3(6)

avg. Turnaround-Time = (24 + 30 + 36) / 3 = 30 time units (this assumes all tasks arrive at time 0)

Task1(6) Task2(6) Task3(24)

avg. Turnaround-Time = (6 +12 +36) / 3 = 18 time units (this assumes all tasks arrive at time 0)

Strengths:

    - Simple
    - Fair
Problems:

    - Convoy Effect
    - Order of task arrival is very important for average Turnaround time
    
#### Shortest Job First (SJF)

Is also NON-PREEMPTIVE. It selects the shortest Job/Process which is available in the run queue. This scheduling algorithm assumes that run times are known in advance.

Strengths:

    - Nearly optimal (Turnaround Time)

Problems:

    - Only optimal if all jobs/process are available simultaneously
    - Usually run times are not known ...
    
#### Shortest Remaining Time Next

Preemptive version of SJF (Shortest Job First). Schedulre picks the job/process which has the lowest remaining time to run.

Strengths:

    - Probably optimal (Tournaround Time)
    
Problems:

    - Run times must be known
    
## Considerations of Designing a Scheduler

[Design a Generic Scheduler https://stackoverflow.com/questions/26094969/design-a-generic-job-scheduler](https://stackoverflow.com/questions/26094969/design-a-generic-job-scheduler)

    - Cancellation - you often want to kill a long running job, or prevent one from running.        
    - Priority - you often want high priority jobs to run in preference to low priority jobs.
      But implementing this in a way that low priority jobs don't wait forever in system where lots of jobs are generated is "non-trivial"      
    - Resources - some jobs may only be schedulable on systems which have certain resources. E.g. some will require large amounts of memory,
      or fast local disk, or fast network access. Allocating these efficiently is tricky.      
    - Dependencies - some jobs may only be runable once other jobs have completed, and thus can not be scheduled before a given time.    
    - Deadlines - some jobs need to be completed by a given time. (or at least be started by a given time.)    
    - Permissions - some users may only be able to submit jobs to certain resource groups, or with certain properties, or a certain number of jobs, etc.    
    - Quotas - some systems give users a specified amount of system time, and running a job subtracts from that. This could make a significant
      difference to the numbers in your example.      
    - Suspension - some systems allow jobs to be check-pointed and suspended and the resumed later.

## Scheduler Framework

    - Quartz (http://www.quartz-scheduler.org/)
    - Chronos (https://github.com/mesos/chronos)
    - Luigi (https://github.com/spotify/luigi)

## Distributed Scheduler

### How Quora Implement a Distributed Cron Scheduler
[Quora Engineering](https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=6&cad=rja&uact=8&ved=0ahUKEwiVrsDx8bbWAhXLs1QKHaazA_QQFghdMAU&url=https%3A%2F%2Fengineering.quora.com%2FQuoras-Distributed-Cron-Architecture&usg=AFQjCNFmkbxNDGZArbj6jnhueXMYl4iNgg)

    - Cron jobs are scheduled on multiple machines.
    - A central database contains job metadata (start time, end time, machine, etc.)
    - Each machine picks the next available task to be executed.