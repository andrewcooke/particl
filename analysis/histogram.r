

rd <- function(i, data) {

  d <- read.table(paste("/home/andrew/projects/personal/particl/data/diff-a-", 
                        i, ".txt", sep=""), header=FALSE)
  diff <- d[[1]]
  l <- length(diff)
  m <- mean(diff)
  s <- sd(diff)

  x <- data$x
  x <- append(x, (sort(diff)-m)/s)

  y <- data$y
  y <- append(y, (1:l)/l)

  n <- data$n
  n <- append(n, rep(i, l))

  list(n=n, x=x, y=y)
}

data <- list(n=c(), x=c(), y=c())
ns = c(5,6,7,8,9,10,15,20,25,30)
for (n in ns) data <- rd(n, data)
data <- data.frame(n=data$n, x=data$x, y=data$y)

require(ggplot2)

pdf("/home/andrew/projects/personal/particl/doc/ecdf-1.pdf",
    width=3, height=3)
qplot(x, y, data=data, geom="step", colour=factor(n),
      xlab='Normalized difference', ylab='ECDF')+
scale_colour_grey(end=0.7,start=0,name='size')
dev.off()

pdf("/home/andrew/projects/personal/particl/doc/ecdf-2.pdf",
    width=3, height=3)
qplot(x, y, data=data, geom="step", colour=factor(n),
      xlim=c(-3, -2), ylim=c(0, 0.03),
      xlab='Normalized difference', ylab='ECDF')+
scale_colour_grey(end=0.7,start=0,name='size')
dev.off()

pdf("/home/andrew/projects/personal/particl/doc/ecdf-3.pdf",
    width=3, height=3)
qplot(x, y, data=data, geom="step", colour=factor(n),
      xlim=c(-3, -2.8), ylim=c(0.0025, 0.0045),
      xlab='Normalized difference', ylab='ECDF')+
scale_colour_grey(end=0.7,start=0,name='size')
dev.off()
