package com.subgraph.orchid.directory;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import com.subgraph.orchid.Router;
import com.subgraph.orchid.RouterDescriptor;
import com.subgraph.orchid.RouterStatus;
import com.subgraph.orchid.TorException;
import com.subgraph.orchid.crypto.TorPublicKey;
import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.data.IPv4Address;
import com.subgraph.orchid.geoip.CountryCodeService;

public class RouterImpl implements Router {
	static RouterImpl createFromRouterStatus(RouterStatus status) {
		return new RouterImpl(status);
	}

	private final HexDigest identityHash;
	protected RouterStatus status;
	private RouterDescriptor descriptor;
	
	private volatile String cachedCountryCode;
	
	protected RouterImpl(RouterStatus status) {
		identityHash = status.getIdentity();
		this.status = status;
	}

	void updateStatus(RouterStatus status) {
		if(!identityHash.equals(status.getIdentity()))
			throw new TorException("Identity hash does not match status update");
		this.status = status;
		this.cachedCountryCode = null;
	}

	void updateDescriptor(RouterDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	public boolean isDescriptorDownloadable() {
		
		if(descriptor != null && descriptor.getDescriptorDigest().equals(status.getDescriptorDigest()))
			return false;
		
		final Date now = new Date();
		final long diff = now.getTime() - status.getPublicationTime().getDate().getTime();
		return diff > (1000 * 60 * 10);	
	}
	
	public String getVersion() {
		return status.getVersion();
	}

	public HexDigest getDescriptorDigest() {
		return status.getDescriptorDigest();
	}

	public IPv4Address getAddress() {
		return status.getAddress();
	}

	public RouterDescriptor getCurrentDescriptor() {
		return descriptor;
	}

	public boolean hasFlag(String flag) {
		return status.hasFlag(flag);
	}

	public boolean isHibernating() {
		if(descriptor == null)
			return false;
		return descriptor.isHibernating();
	}

	public boolean isRunning() {
		return hasFlag("Running");
	}

	public boolean isValid() {
		return hasFlag("Valid");
	}

	public boolean isBadExit() {
		return hasFlag("BadExit");
	}

	public boolean isPossibleGuard() {
		return hasFlag("Guard");
	}

	public boolean isExit() {
		return hasFlag("Exit");
	}

	public boolean isFast() {
		return hasFlag("Fast");
	}

	public boolean isStable() {
		return hasFlag("Stable");
	}
	
	public boolean isHSDirectory() {
		return hasFlag("HSDir");
	}

	public int getDirectoryPort() {
		return status.getDirectoryPort();
	}

	public HexDigest getIdentityHash() {
		return identityHash;
	}
	
	public TorPublicKey getIdentityKey() {
		if(descriptor != null) {
			return descriptor.getIdentityKey();
		} else {
			return null;
		}
	}

	public String getNickname() {
		return status.getNickname();
	}

	public int getOnionPort() {
		return status.getRouterPort();
	}

	public TorPublicKey getOnionKey() {
		if(descriptor != null) {
			return descriptor.getOnionKey();
		} else {
			return null;
		}
	}

	public boolean hasBandwidth() {
		return status.hasBandwidth();
	}

	public int getEstimatedBandwidth() {
		return status.getEstimatedBandwidth();
	}

	public int getMeasuredBandwidth() {
		return status.getMeasuredBandwidth();
	}

	public Set<String> getFamilyMembers() {
		if(descriptor == null) {
			return Collections.emptySet();
		}
		return descriptor.getFamilyMembers();
	}
	
	public int getAverageBandwidth() {
		if(descriptor == null)
			return 0;
		return descriptor.getAverageBandwidth();
	}

	public int getBurstBandwidth() {
		if(descriptor == null)
			return 0;
		return descriptor.getBurstBandwidth();
	}

	public int getObservedBandwidth() {
		if(descriptor == null)
			return 0;
		return descriptor.getObservedBandwidth();
	}

	public boolean exitPolicyAccepts(IPv4Address address, int port) {
		if(descriptor == null)
			return false;

		if(address == null)
			return descriptor.exitPolicyAccepts(port);

		return descriptor.exitPolicyAccepts(address, port);
	}

	public boolean exitPolicyAccepts(int port) {
		return exitPolicyAccepts(null, port);
	}
	
	public String toString() {
		return "Router["+ getNickname() +" ("+getAddress() +":"+ getOnionPort() +")]";
	}

	public String getCountryCode() {
		String cc = cachedCountryCode;
		if(cc == null) {
			cc = CountryCodeService.getInstance().getCountryCodeForAddress(getAddress());
			cachedCountryCode = cc;
		}
		return cc;
	}
}
