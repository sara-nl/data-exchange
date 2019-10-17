
Current approach:

```bash
strace -f sh -c 'python test.py > out.txt 2> error.txt' |& b3 | jq 'select(.syscall == "openat")'
```

### TODO

### Issues

* OSX: `dtrace: system integrity protection is on, some features will not be available`. For instance, `printf("%s %s\n", execname, copyinstr(arg0));` doesn't work.
* Update the message format to support runtime info
* Make sure Docker image has `dtrace` installed. (E.g.: `apt-get update && apt-get install systemtap systemtap-runtime systemtap-sdt-dev`);
* Make sure the script can make its way from the code repository into the secure container;
* Pass the process ID into `dtrace` command;
* Kill DTrace (or let it exit when the process finishes);

### Testing

* Successfully open files should be present in the output
* Blocked attempts should be present in the output
* Successful newtwork connections should be present in the output ???
* Blocked network connections should be present in the output

### Useful links for understanding how Dtrace works

* [D Program Structure](http://dtrace.org/guide/chp-prog.html)
* [Tutorial: DTrace by Example
](https://www.oracle.com/technetwork/server-storage/solaris10/dtrace-tutorial-142317.html#Intro)