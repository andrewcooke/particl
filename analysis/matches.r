
rd <- function(x, data) {

  f <- paste("/home/andrew/project/particl/data/dump-", x, "-a-*.best", sep="")
  print(f)
  f <- Sys.glob(f)
  print(f)
  d <- read.table(f, header=FALSE)

  nmatches <- data$nmatches
  nmatches <- append(nmatches, d[[3]])

  delta2 <- data$delta2
  delta2 <- append(delta2, d[[4]])

  n <- data$n
  n <- append(n, rep(x, length(d[[1]])))

  list(n=n, nmatches=nmatches, delta2=delta2)
}

data <- list(n=c(), nmatches=c(), delta2=c())
ns <- c(5,6,7,8,9,10,11,12,13)
for (n in ns) data <- rd(n, data)
data <- data.frame(n=data$n, nmatches=data$nmatches, delta2=data$delta2)

require(ggplot2)

qplot(delta2/n^2, factor(n), data=data, colour=delta2, geom="jitter",
      xlim=c(-1, 5))
