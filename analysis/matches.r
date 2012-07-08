
rd <- function(x, data) {

  f <- paste("/home/andrew/project/particl/data/dump-", x, "-a-*.best", sep="")
  print(f)
  f <- Sys.glob(f)
  print(f)
  d <- read.table(f, header=FALSE)

  nbits <- data$nbits
  nb <- as.numeric(stringr::str_split(f, "-")[[1]][4])
  nbits <- append(nbits, rep(nb, length(d[[1]])))

  nmatches <- data$nmatches
  nmatches <- append(nmatches, d[[3]])

  delta2 <- data$delta2
  delta2 <- append(delta2, d[[4]])

  n <- data$n
  n <- append(n, rep(x, length(d[[1]])))

  list(n=n, nmatches=nmatches, nbits=nbits, delta2=delta2)
}

data <- list(n=c(), nmatches=c(), nbits=c(), delta2=c())
ns <- c(5,6,7,8,9,10,11,12,13,14,15)
for (n in ns) data <- rd(n, data)
data <- data.frame(n=data$n, nmatches=data$nmatches, nbits=data$nbits, delta2=data$delta2)

require(ggplot2)

qplot(delta2/n^2, factor(n), data=data, colour=factor(nbits), geom="jitter",
      xlim=c(0, 3))
