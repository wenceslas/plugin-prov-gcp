package org.ligoj.app.plugin.prov.google;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.transaction.Transactional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.plugin.prov.ProvResource;
import org.ligoj.app.plugin.prov.QuoteVo;
import org.ligoj.app.plugin.prov.model.ProvInstanceType;
import org.ligoj.app.plugin.prov.model.ProvInstancePrice;
import org.ligoj.app.plugin.prov.model.ProvInstancePriceTerm;
import org.ligoj.app.plugin.prov.model.ProvQuote;
import org.ligoj.app.plugin.prov.model.ProvQuoteInstance;
import org.ligoj.app.plugin.prov.model.ProvQuoteStorage;
import org.ligoj.app.plugin.prov.model.ProvStorageFrequency;
import org.ligoj.app.plugin.prov.model.ProvStoragePrice;
import org.ligoj.app.plugin.prov.model.ProvStorageType;
import org.ligoj.app.plugin.prov.model.VmOs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test class of {@link ProvGoogleResource}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class ProvGoogleResourceTest extends AbstractAppTest {

	@Autowired
	private ProvGoogleResource resource;

	@Autowired
	private ProvResource provResource;

	private int subscription;

	@Before
	public void prepareData() throws IOException {
		persistSystemEntities();
		persistEntities("csv",
				new Class[] { Node.class, Project.class, Subscription.class, ProvQuote.class, ProvStorageType.class,
						ProvInstancePriceTerm.class, ProvInstanceType.class, ProvInstancePrice.class,
						ProvQuoteInstance.class, ProvQuoteStorage.class },
				StandardCharsets.UTF_8.name());
		subscription = getSubscription("gStack", ProvGoogleResource.SERVICE_KEY);
	}

	@Test
	public void getConfiguration() {
		final QuoteVo vo = provResource.getConfiguration(subscription);
		Assert.assertEquals("quote1", vo.getName());
		Assert.assertNotNull(vo.getId());
		Assert.assertNotNull(vo.getCreatedBy());
		Assert.assertNotNull(vo.getCreatedDate());
		Assert.assertNotNull(vo.getLastModifiedBy());
		Assert.assertNotNull(vo.getLastModifiedDate());

		// Check compute
		final List<ProvQuoteInstance> instances = vo.getInstances();
		Assert.assertEquals(3, instances.size());
		final ProvQuoteInstance quoteInstance = instances.get(0);
		Assert.assertNotNull(quoteInstance.getId());
		Assert.assertEquals("f1-micro-LINUX-Default-0.0086", quoteInstance.getName());
		final ProvInstancePrice instancePrice = quoteInstance.getPrice();
		Assert.assertEquals(0.0086, instancePrice.getCost(), 0.0001);
		Assert.assertEquals(VmOs.LINUX, instancePrice.getOs());
		Assert.assertNotNull(instancePrice.getTerm().getId());
		Assert.assertEquals(1, instancePrice.getTerm().getPeriod().intValue());
		Assert.assertEquals("Default", instancePrice.getTerm().getName());
		final ProvInstanceType instance = instancePrice.getType();
		Assert.assertNotNull(instance.getId().intValue());
		Assert.assertEquals("f1-micro", instance.getName());
		Assert.assertEquals(0.2, instance.getCpu(), 0.01);
		Assert.assertEquals(614, instance.getRam().intValue());
		Assert.assertFalse(instance.getConstant());
		
		Assert.assertEquals("SQL Server Enterprise",instances.get(2).getPrice().getLicense());

		// Check storage
		final List<ProvQuoteStorage> storages = vo.getStorages();
		Assert.assertEquals(4, storages.size());
		final ProvQuoteStorage quoteStorage = storages.get(0);
		Assert.assertNotNull(quoteStorage.getId());
		Assert.assertEquals("server1-root", quoteStorage.getName());
		Assert.assertEquals(20, quoteStorage.getSize().intValue());
		Assert.assertNotNull(quoteStorage.getQuoteInstance());
		final ProvStoragePrice storage = quoteStorage.getPrice();
		Assert.assertNotNull(storage.getId());
		Assert.assertEquals(0.04, storage.getCostGb(), 0.001);
		Assert.assertEquals(0, storage.getCost(), 0.001);
		Assert.assertEquals("Standard provisioned space", storage.getType().getName());
		Assert.assertEquals(ProvStorageFrequency.COLD, storage.getType().getFrequency());

		// Not attached storage
		Assert.assertNull(storages.get(3).getQuoteInstance());

	}

	@Test
	public void getKey() {
		Assert.assertEquals("service:prov:google", resource.getKey());
	}

}
