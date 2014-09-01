package at.ac.testing.brooklyn.utils;

import java.util.List;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractEntity;
import brooklyn.entity.trait.Resizable;
import brooklyn.entity.trait.Startable;
import brooklyn.test.entity.TestCluster;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * This is a literal copy of
 * {@link brooklyn.policy.autoscaling.LocallyResizableEntity} that we moved in
 * this package to enable proper testing. Some fields are declared as protected
 * but we needed them to be accessible during the tests
 * 
 * @author alessiogambi
 * 
 */
public class LocallyResizableEntity extends AbstractEntity implements Resizable {
	TestCluster cluster;
	public List<Integer> sizes = Lists.newArrayList();
	public long resizeSleepTime = 0;

	public LocallyResizableEntity(TestCluster tc) {
		this(null, tc);
	}

	public LocallyResizableEntity(Entity parent, TestCluster tc) {
		super(parent);
		this.cluster = tc;
		setAttribute(Startable.SERVICE_UP, true);
	}

	@Override
	public Integer resize(Integer newSize) {
		try {
			Thread.sleep(resizeSleepTime);
			sizes.add(newSize);
			return cluster.resize(newSize);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw Throwables.propagate(e);
		}
	}

	@Override
	public Integer getCurrentSize() {
		return cluster.getCurrentSize();
	}

	@Override
	public String toString() {
		return getDisplayName();
	}

}
