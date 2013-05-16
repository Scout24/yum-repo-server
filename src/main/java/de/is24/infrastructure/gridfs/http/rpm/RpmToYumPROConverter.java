package de.is24.infrastructure.gridfs.http.rpm;

import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFormatEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageRequirement;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageVersion;
import de.is24.infrastructure.gridfs.http.exception.InvalidRpmHeaderException;
import java.util.ArrayList;
import java.util.List;
import static de.is24.infrastructure.gridfs.http.rpm.RpmPROTagFlagsToYumConverter.convert;
import static de.is24.infrastructure.gridfs.http.rpm.RpmPROTagFlagsToYumConverter.isPre;
import static de.is24.infrastructure.gridfs.http.rpm.RpmPROTagVersionToYumConverter.convert;
import static org.freecompany.redline.header.Header.HeaderTag.CONFLICTFLAGS;
import static org.freecompany.redline.header.Header.HeaderTag.CONFLICTNAME;
import static org.freecompany.redline.header.Header.HeaderTag.CONFLICTVERSION;
import static org.freecompany.redline.header.Header.HeaderTag.OBSOLETEFLAGS;
import static org.freecompany.redline.header.Header.HeaderTag.OBSOLETENAME;
import static org.freecompany.redline.header.Header.HeaderTag.OBSOLETEVERSION;
import static org.freecompany.redline.header.Header.HeaderTag.PROVIDEFLAGS;
import static org.freecompany.redline.header.Header.HeaderTag.PROVIDENAME;
import static org.freecompany.redline.header.Header.HeaderTag.PROVIDEVERSION;
import static org.freecompany.redline.header.Header.HeaderTag.REQUIREFLAGS;
import static org.freecompany.redline.header.Header.HeaderTag.REQUIRENAME;
import static org.freecompany.redline.header.Header.HeaderTag.REQUIREVERSION;


public class RpmToYumPROConverter {
  private final RpmHeaderWrapper rpmHeaderWrapper;

  public RpmToYumPROConverter(final RpmHeaderWrapper rpmHeaderWrapper) {
    this.rpmHeaderWrapper = rpmHeaderWrapper;
  }

  public List<YumPackageFormatEntry> convertProvides() throws InvalidRpmHeaderException {
    return createEntries(
      rpmHeaderWrapper.readStrings(PROVIDENAME, false),
      rpmHeaderWrapper.readIntegers(PROVIDEFLAGS, false),
      rpmHeaderWrapper.readStrings(PROVIDEVERSION, false));
  }

  public List<YumPackageRequirement> convertRequires() throws InvalidRpmHeaderException {
    return createRequirementEntries(
      rpmHeaderWrapper.readStrings(REQUIRENAME, false),
      rpmHeaderWrapper.readIntegers(REQUIREFLAGS, false),
      rpmHeaderWrapper.readStrings(REQUIREVERSION, false));
  }

  public List<YumPackageFormatEntry> convertObsoletes() throws InvalidRpmHeaderException {
    return createEntries(
      rpmHeaderWrapper.readStrings(OBSOLETENAME, false),
      rpmHeaderWrapper.readIntegers(OBSOLETEFLAGS, false),
      rpmHeaderWrapper.readStrings(OBSOLETEVERSION, false));
  }

  public List<YumPackageFormatEntry> convertConflicts() throws InvalidRpmHeaderException {
    return createEntries(
      rpmHeaderWrapper.readStrings(CONFLICTNAME, false),
      rpmHeaderWrapper.readIntegers(CONFLICTFLAGS, false),
      rpmHeaderWrapper.readStrings(CONFLICTVERSION, false));
  }

  private List<YumPackageFormatEntry> createEntries(final String[] names, final int[] flags, final String[] versions)
                                             throws InvalidRpmHeaderException {
    final List<YumPackageFormatEntry> entries = new ArrayList<>(names.length);
    for (int i = 0; i < names.length; i++) {
      entries.add(createYumFormatEntry(names[i], flags[i], versions[i]));
    }
    return entries;
  }

  private List<YumPackageRequirement> createRequirementEntries(final String[] names, final int[] flags,
                                                               final String[] versions)
                                                        throws InvalidRpmHeaderException {
    final List<YumPackageRequirement> entries = new ArrayList<>(names.length);
    for (int i = 0; i < names.length; i++) {
      entries.add(createYumRequirementEntry(names[i], flags[i], versions[i]));
    }
    return entries;
  }

  private YumPackageFormatEntry createYumFormatEntry(final String name, final int flag, final String version)
                                              throws InvalidRpmHeaderException {
    YumPackageFormatEntry packageFormatEntry = new YumPackageFormatEntry();
    packageFormatEntry.setName(name);
    packageFormatEntry.setFlags(translateFlags(flag));
    packageFormatEntry.setVersion(createVersion(version));
    return packageFormatEntry;
  }

  private YumPackageRequirement createYumRequirementEntry(final String name, final int flag, final String version)
                                                   throws InvalidRpmHeaderException {
    YumPackageRequirement requirementEntry = new YumPackageRequirement();
    requirementEntry.setName(name);
    requirementEntry.setFlags(translateFlags(flag));
    requirementEntry.setVersion(createVersion(version));
    requirementEntry.setPre(isPre(flag));
    return requirementEntry;
  }

  private YumPackageVersion createVersion(final String provideVersion) {
    return convert(provideVersion);
  }

  private String translateFlags(final int provideFlag) throws InvalidRpmHeaderException {
    return convert(provideFlag);
  }
}
