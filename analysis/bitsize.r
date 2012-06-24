
rd <- function(x, data) {
  d <- read.table(paste("/tmp/diff-2b-a-", x, ".txt", sep=""), header=FALSE)
  bits <- data$bits
  bits <- append(bits, d[[2]])
  bits <- append(bits, d[[3]])
  n <- data$n
  n <- append(n, rep(x, length(d[[2]])))
  n <- append(n, rep(x, length(d[[3]])))
  list(n=n, bits=bits)
}

data <- list(n=c(), bits=c())
ns <- c(5,6,7,8,9,10,15,20,25,30)
for (n in ns) data <- rd(n, data)
data <- data.frame(n=data$n, bits=data$bits)

require(ggplot2)

pdf("/home/andrew/projects/personal/particl/doc/bits.pdf",
    width=3, height=3)
qplot(factor(n), bits/n**2, data=data, geom="violin",
      xlab="Matrix size", ylab="Bits per builder iteration")
dev.off()
