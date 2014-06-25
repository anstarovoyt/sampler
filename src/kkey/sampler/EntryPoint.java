package kkey.sampler;


import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Start debuggable app with args
 * -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=7896
 */
public class EntryPoint {

  public static final String PORT = "7896";
  public static final String HOST = "127.0.0.1";

  private final static class Frame implements Comparable<Frame> {

    public Frame(String value) {
      frame = value;
    }

    public final String frame;
    public int counter = 0;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;

      return frame.equals(((Frame)o).frame);
    }

    @Override
    public int hashCode() {
      return frame.hashCode();
    }

    @Override
    public int compareTo(Frame o) {
      return -Integer.compare(counter, o.counter);
    }
  }

  private HashMap<String, Frame> frames = new HashMap<>();

  public static void main(String[] arg) throws Exception {
    VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
    AttachingConnector connector = findConnector(virtualMachineManager);
    Map<String, Connector.Argument> args = connector.defaultArguments();
    args.get("port").setValue(PORT);
    args.get("hostname").setValue(HOST);
    VirtualMachine vm = connector.attach(args);

    new EntryPoint().sampling(vm);
  }

  private void sampling(VirtualMachine vm) throws Exception {
    IntStream.range(0, 100).forEach(i -> {
      try {
        Thread.sleep(100);

        vm.suspend();
        for (ThreadReference threadReference : vm.allThreads()) {
          for (StackFrame stackFrame : threadReference.frames()) {
            Frame frame = getFrame(stackFrame.location().toString());
            frame.counter++;
          }
        }
        vm.resume();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    ArrayList<Frame> values = new ArrayList<>(frames.values());
    Collections.sort(values);

    IntStream.rangeClosed(0, 5).mapToObj(i -> values.get(i).frame).forEach(System.out::println);
  }

  private Frame getFrame(String stringLocation) {
    return frames.computeIfAbsent(stringLocation, Frame::new);
  }


  private static AttachingConnector findConnector(VirtualMachineManager virtualMachineManager) {
    Optional<AttachingConnector> first = virtualMachineManager.attachingConnectors()
      .stream()
      .filter(connector -> "dt_socket".equalsIgnoreCase(connector.transport().name()))
      .findFirst();

    return first.get();
  }
}
