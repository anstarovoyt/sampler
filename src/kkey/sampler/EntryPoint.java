package kkey.sampler;


import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EntryPoint {

  public static final String PORT = "7896";
  public static final String HOST = "127.0.0.1";

  private final static class Frame implements Comparable<Frame>{
    public String frame;
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
    for (int i = 0; i < 100; i++) {
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

    ArrayList<Frame> values = new ArrayList<>(frames.values());
    Collections.sort(values);

    for (int i = 0; i < 5; i++) {
      System.out.println(values.get(i).frame);
    }
  }

  private Frame getFrame(String stringLocation) {
    Frame frame = frames.get(stringLocation);
    if (frame == null) {
      frame = new Frame();
      frame.frame = stringLocation;
      frames.put(stringLocation, frame);
    }

    return frame;
  }


  private static AttachingConnector findConnector(VirtualMachineManager virtualMachineManager) {
    for (AttachingConnector attachingConnector : virtualMachineManager.attachingConnectors()) {
      if ("dt_socket".equalsIgnoreCase(attachingConnector.transport().name())) return attachingConnector;
    }

    throw new IllegalStateException("cannot find connector");
  }
}
