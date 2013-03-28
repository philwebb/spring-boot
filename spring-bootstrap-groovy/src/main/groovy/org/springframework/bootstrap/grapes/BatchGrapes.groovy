package org.springframework.bootstrap.grapes

@GrabResolver(name='spring-milestone', root='http://repo.springframework.org/milestone')
@GrabResolver(name='spring-snapshot', root='http://repo.springframework.org/snapshot')
@GrabConfig(systemClassLoader=true)
@Grab("org.springframework.bootstrap:spring-bootstrap:0.0.1-SNAPSHOT")
@Grab("org.springframework.batch:spring-batch-core:2.2.0.M1")
@Grab("org.springframework:spring-context:3.2.2.BOOTSTRAP-SNAPSHOT")
class BatchGrapes {
}

import org.springframework.bootstrap.autoconfigure.ConditionalOnMissingBean
import org.springframework.bootstrap.CommandlineRunner
import org.springframework.batch.core.Job
import org.springframework.batch.core.converter.DefaultJobParametersConverter
import org.springframework.batch.core.converter.JobParametersConverter
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.util.StringUtils
import groovy.util.logging.Log

@Configuration
@ConditionalOnMissingBean(CommandlineRunner)
@Log
class BatchCommand {

  @Autowired(required=false)
  private JobParametersConverter converter = new DefaultJobParametersConverter()

  @Autowired
  private JobLauncher jobLauncher

  @Autowired
  private Job job

  @Bean
  CommandlineRunner batchCommandlineRunner() { 
    return new CommandlineRunner() { 
      void run(String... args) {
        log.info("Running default command line with: ${args}")
        launchJobFromProperties(StringUtils.splitArrayElementsIntoProperties(args, "="))
      }
    }
  }

  protected void launchJobFromProperties(Properties properties) { 
    jobLauncher.run(job, converter.getJobParameters(properties))
  }

}
