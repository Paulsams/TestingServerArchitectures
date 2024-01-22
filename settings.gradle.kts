rootProject.name = "TestingServerArchitectures"

include("messages")
include("serverArchitectures")
include("serverArchitectures:blocking")
findProject(":serverArchitectures:blocking")?.name = "blocking"
include("serverArchitectures:non-blocking")
findProject(":serverArchitectures:non-blocking")?.name = "non-blocking"
include("serverArchitectures:asynchronous")
findProject(":serverArchitectures:asynchronous")?.name = "asynchronous"
include("application")
include("client")
include("overallConfiguration")
include("serverArchitectures:nioUtils")
findProject(":serverArchitectures:nioUtils")?.name = "nioUtils"
include("combinerCharts")
