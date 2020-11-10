package xyz.yooniks.aegis.vpn;

import java.util.List;

public class VPNDetectorClearLimitTask implements Runnable {

  private final List<VPNDetector> detectors;

  public VPNDetectorClearLimitTask(List<VPNDetector> detectors) {
    this.detectors = detectors;
  }

  @Override
  public void run() {
    this.detectors.stream()
        .filter(VPNDetector::isLimitable)
        .forEach(VPNDetector::clearCount);
  }

}
