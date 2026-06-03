$env:JAVA_HOME = 'C:\Users\VALENTINA BARRETO\.jdks\ms-21.0.11'
$env:Path = $env:JAVA_HOME + '\bin;' + $env:Path
Set-Location 'C:\Users\VALENTINA BARRETO\IdeaProjects\ProyectoNuclearPracticas'
.\mvnw.cmd '-Dspring-boot.run.profiles=h2' spring-boot:run
