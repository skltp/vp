package se.skl.tp.vp.service;

import java.util.Date;
import lombok.Data;

@Data
public class HsaCacheStatus {
  boolean initialized;
  Date resetDate;
  int numInCacheOld;
  int numInCacheNew;
}