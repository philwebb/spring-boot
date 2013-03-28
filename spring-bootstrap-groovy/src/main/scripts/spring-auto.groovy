// Get the args and turn them into classes
def configs = []
def parameters = []
args.each { arg ->
  if (arg.endsWith(".class")) {
    configs << arg.replaceAll(".class", "")
  } else {
    parameters << arg
  }
}

// Dynamically grab some dependencies
def dependencySource = "org.springframework.bootstrap.grapes.Dependencies" as Class // TODO: maybe strategise this
def dependencies = [*dependencySource.defaults(), *dependencySource.dependencies(configs)]
configs = dependencies + configs

// Do this before any Spring auto stuff is used in case it enhances the classpath
configs = configs as Class[]
parameters = parameters as String[]

// Now create a Spring context
def selectorType = "org.springframework.bootstrap.context.ApplicationContextSelector" as Class
def ctx = selectorType.select(configs)
ctx.refresh()

def runner = null

try {
      
  def runnerType = "org.springframework.bootstrap.CommandlineRunner" as Class
  runner = ctx.getBean(runnerType)

} catch (Exception e) {
  log.info("No CommandlineRunner is defined (${e})")
}

if (runner!=null) { 
  runner.run(parameters)
}

