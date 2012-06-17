
dohist <- function(n) {
 a <- read.csv(paste("/tmp/diff-a-", n, ".txt", sep=""), header=FALSE)
 a <- a[[1]]
 s <- sd(a)
 m <- mean(a)
 hist(a, 50, prob=T, main=paste("Diff, size", n))
 f <- function(x) dnorm(x, mean=m, sd=s)
 curve(f, m-5*s, m+5*s, add=T)
 qqnorm(a)
 qqline(a, col="red")
 # ks.test(a, "pnorm", m, s) # but uses estimated parameter
 # shapiro.test(sample(a,5000))
}

par(ask=T)
for (n in seq(15, 30, 5)) {
  dohist(n)
}

for (n in 7:10) {
  dohist(n)
}

dohist(30)

compare <- function(n, m) {
 a <- read.csv(paste("/tmp/diff-a-", n, ".txt", sep=""), header=FALSE)
 a <- a[[1]]
 b <- read.csv(paste("/tmp/diff-a-", m, ".txt", sep=""), header=FALSE)
 b <- b[[1]]
 qqplot(a, b)
}

compare(5,30)
compare(10,30)

a7 <- read.csv("/tmp/diff-a-7.txt", header=FALSE)
a7 <- a7[[1]]
a30 <- read.csv("/tmp/diff-a-30.txt", header=FALSE)
a30 <- a30[[1]]
a7s <- sort(a7)
a30s <- sort(a30)
rs <- a30s / a7s
a30sm <- a30s - mean(a30s)
a7sm <- a7s - mean(a7s)
a30sms <- a30sm / sd(a30sm)
a7sms <- a7sm / sd(a7sm)
plot(a30sms,a7sms)

readnorm <- function(n) {
 a <- read.csv(paste("/tmp/diff-a-", n, ".txt", sep=""), 
               col.names=c("raw"), header=FALSE)
 a <- list(n=n, raw=a$raw, median=median(a$raw), iqr=IQR(a$raw))
 a$shifted <- a$raw - a$median
 a$scaled <- a$raw / a$iqr
 a$normed <- a$shifted / a$iqr
 a$necdf <- ecdf(a$normed)
 return(a)
}

ns = c(5,6,7,8,9,10,15,20,25,30)
data <- lapply(ns,readnorm)


rd <- function(n) {
  read.table(paste("/tmp/diff-a-", n, ".txt", sep=""), header=FALSE)
}

ns = c(5,6,7,8,9,10,15,20,25,30)
d <- do.call(cbind, lapply(ns, rd))
colnames(d) <- paste(ns)

d_shift <- scale(d, center=apply(d, 2, median), scale=FALSE)
d_norm <- scale(d, center=apply(d, 2, median), scale=apply(d, 2, IQR))

plot(ecdf(d_norm[,1]),xlim=c(-5,-2),ylim=c(0,0.01))
lapply(seq(2:dim(d_norm)[2]),function(n) {lines(ecdf(d_norm[,n]))})



ns3 = c(5,6,7,8,9,10)
d1 <- do.call(cbind, lapply(ns3, rd))
colnames(d1) <- paste(ns3)

rd3 <- function(n) {
  read.table(paste("/tmp/diff3-a-", n, ".txt", sep=""), header=FALSE)
}
d3 <- do.call(cbind, lapply(ns3, rd3))
colnames(d3) <- paste(ns3)

d1_norm <- scale(d1, center=apply(d1, 2, median), scale=apply(d1, 2, IQR))
d3_norm <- scale(d3, center=apply(d3, 2, median), scale=apply(d3, 2, IQR))

plot(ecdf(d1_norm[,1]),xlim=c(-5,-2),ylim=c(0,0.01))
plot(ecdf(d1_norm[,1]))
lapply(seq(2,dim(d1_norm)[2]),function(n) {lines(ecdf(d1_norm[,n]))})

lapply(seq(1,dim(d3_norm)[2]),function(n) {lines(ecdf(d3_norm[,n]))})

plot(ecdf(d3_norm[,1]),xlim=c(-5,-2),ylim=c(0,0.01))
lapply(seq(2,dim(d3_norm)[2]),function(n) {lines(ecdf(d3_norm[,n]))})

