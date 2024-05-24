library(readr) # For reading CSV files
library(dplyr) # For data manipulation
library(ggplot2) # For data visualization
data <- read_csv("C:\\Users\\venka\\Downloads\\listings.csv")
str(data) #stratifies the data
summary(data)
###################################
#Dataset 1
data$price <- as.numeric(gsub("\\$", "", data$price))
data$price[is.na(data$price)]<-mean(data$price,na.rm=TRUE)
print(data$price)
# Removes all na values and replaces with mean
summary(data$price) #five number summary
boxplot(data$price)
# From the given box plot we see that there are a considerable number of outliers in the data set. We will clean the data by removing them and then plotting another box plot. To view the skewness better we will plot a histogram of the same dataset. The box plot and histogram show that most hotels/homestays charge their customers around 135 dollars (skewed normal distribution uses median as central tendency).
histogram_data1 <- ggplot(data, aes(x = price)) +
  geom_histogram(binwidth = 100, fill = "skyblue", color = "black") +
  labs(title = "Histogram of Price (with outliers)")
print(histogram_data1)
#The histogram shows that the data is right-skewed and that there are a few high-end hotels which charge high prices while most hotels range between 100 and 200 dollars.
############
# Now we will show the data plot without outliers
Q1<-quantile(data$price, .25, na.rm=TRUE)
print(Q1)
Q3<-quantile(data$price, .75, na.rm=TRUE)
print(Q3)
d<-IQR(data$price,na.rm=TRUE)
d
no_outliers<- data %>%
  filter(data$price>(Q1-1.5*d) & data$price<(Q3+1.5*d))
boxplot_data <- ggplot(no_outliers, aes(y = price)) +
  geom_boxplot() +
  labs(title = "Boxplot of Price Without Outliers")
print(boxplot_data)
histogram_data <- ggplot(no_outliers, aes(x = price)) +
  geom_histogram(binwidth = 100, fill = "skyblue", color = "black") +
  labs(title = "Histogram of Price Without Outliers")
print(histogram_data)
# Now after removing the outliers we see that the histogram assumes an almost normal shape. This is because all the high-end hotel prices have been filtered out.
#############
#dataset 2
data$review_scores_rating[is.na(data$review_scores_rating)]<-mean(data$review_scores_rating,na.rm=TRUE)
summary(data$review_scores_rating)
boxplot(data$review_scores_rating)
bar_data <- ggplot(data, aes(x = review_scores_rating)) +
  geom_bar(stat="count", fill = "skyblue", color = "black") +
  labs(title = "Bar of ratings (with outliers)")
print(bar_data)
#The box plot and the bar graph for ratings show us that most of the ratings lie between 4.5 and 5 and few lie between 4 and 4.5. Consumer dissatisfaction is very low, and this shows that the hotels provide very good quality of service. I will look to demonstrate that in such kinds of data, removal of outliers is a bad practice, as most data in this case will be considered outliers and so we will miss out on a major chunk of the analysis.
##############
Q1<-quantile(data$review_scores_rating, .25, na.rm=TRUE)
print(Q1)
Q3<-quantile(data$review_scores_rating, .75, na.rm=TRUE)
print(Q3)
d<-IQR(data$review_scores_rating,na.rm=TRUE)
d
# The IQR being so small means that any data beyond around 4.95 and any data below around 4.62 will not be taken into account. From the bar graph we saw that a large number of customers gave their hotel stays a 5 star rating. All of that will be disregarded if we remove outliers.
#Most likely an attempt to print the boxplot or the bar graph in this scenario will give an empty result.
no_outliers<- data %>%
  filter(data$price>(Q1-1.5*d) & data$price<(Q3+1.5*d))
boxplot_data <- ggplot(no_outliers, aes(y = price)) +
  geom_boxplot() +
  labs(title = "Boxplot of Price Without Outliers")
print(boxplot_data)
###################
#dataset 3
#this dataset will deal with categorical data and how to visualise it.
summary(data$neighbourhood_cleansed)
neighbourhood<- data %>%
  group_by(neighbourhood_cleansed) %>%
  summarise(count= n()) %>%
  arrange(desc(count))
ggplot(neighbourhood, aes(x = neighbourhood_cleansed, y = count)) +
  geom_bar(stat = "identity", fill = "skyblue", color = "black") +
  labs(x = "Neighbourhood", y = "count", title = "Number of Rentals in Neighbourhood") +
  theme(axis.text.x = element_text(angle = 90, vjust = 0.5, hjust = 1))
print(neighbourhood)
#to show all rows
print(neighbourhood, n=223)
#This data above shows that most hotels have preferred to set up in certain popular neighbourhoods(The top 10 neighbourhoods have over a 1000 hotels while we see a sharp decline from there on). This could be for a multitude of reasons. Since they have to be setup around tourist attractions, it is possible the neighbourhoods with high hotel concentration have more tourist spots. While the variation of number of hotels is very high, none of these can be considered outliers as this statistic deals with the extremes. When researching on where to set up a new hotel, it would be wise to avoid both extremes as one end shows that hotels do not see business in certain neighbourhoods, and the other end shows that setting up a hotel in the locality will come with high competition, and could be bad for a start up business. While we will never set up in the lower end, we should eventually look to set up a business in a neighbourhood like Bedford-Stuyvesant after establishing the business properly.
#############
#These are just analyses of a few data points from the given dataset. A thourough analysis of every data point will show us exactly what the optimal business plan for a new hotel venture will be. Not only that, by analysing columns like no_of_reviews we can approximately guess which hotels turnover more customers, which ones dont, which hotels are new and so on.We could even combine two different analyses to come up with new inferences. For instance, if we combine the prices and the reviews, we can see which hotel in a certain price range is the best. Combining this with neighbourhood will give the most cost-efficient yet high quality option- thus the reason for choosing these 3 specific datasets.