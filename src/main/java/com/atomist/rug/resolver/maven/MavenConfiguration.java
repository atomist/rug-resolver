package com.atomist.rug.resolver.maven;

import java.util.concurrent.ExecutorService;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.SyncContextFactory;
import org.eclipse.aether.impl.UpdateCheckManager;
import org.eclipse.aether.impl.UpdatePolicyAnalyzer;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.io.FileProcessor;
import org.eclipse.aether.spi.locator.Service;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.atomist.rug.resolver.concurrent.MdcThreadPoolExecutor;

import io.takari.aether.localrepo.TakariUpdateCheckManager;
import io.takari.filemanager.FileManager;
import io.takari.filemanager.internal.DefaultFileManager;

@Configuration
@EnableConfigurationProperties(MavenProperties.class)
public class MavenConfiguration {

    @Bean
    @Qualifier("maven-resolver-pool")
    public ExecutorService mavenExecutorService() {
        return MdcThreadPoolExecutor.newFixedThreadPool(10, "maven-resolver-pool");
    }

    @Bean
    public RepositorySystem repositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();

        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(FileManager.class, DefaultFileManager.class);

        locator.setService(SyncContextFactory.class, LockingSyncContextFactory.class);
        locator.setService(FileProcessor.class, LockingFileProcessor.class);
        locator.setService(UpdateCheckManager.class, ServiceEnabledTakariUpdateCheckManager.class);

        return locator.getService(RepositorySystem.class);
    }
    
    public static class ServiceEnabledTakariUpdateCheckManager extends TakariUpdateCheckManager implements Service {

        @Override
        public void initService(ServiceLocator locator) {
            setUpdatePolicyAnalyzer(locator.getService(UpdatePolicyAnalyzer.class));
        }
    }
}
