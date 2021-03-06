# inv-mgmt
An inventory management system using Postgres & JDBC. 

We implement a range of features that provide a scenario in which we can explore the capabilities and uses of procedures, views, functions and triggers in Postgres, while we attempt to reduce the need entirely for SQL embedded within Java.  

## Solutions

### Option 1, 2 and 3
Option 1, 2 and 3 are very similar. In our code, we can think of 2 and 3 being extensions of 1. 
In order to use these similarities to our advantage in our code, we split out as much logic as possible that is shared between the 3 methods, and placed it in it's own classes and functions. Some examples of this include:
- *Class Order* - A class designed to handle the information needed for creating an order (Option 1, 2, 3). The class takes an *orderType* value as an argument to it's constructor, which allows it to determine what data is needs to collect, when we call the primary method *handleOption()*. This option asks the user in all 3 cases for the basic required information, and then select additional information depending on whether it is handling an option 2 or 3 order.
Ideally, we would have been able to pass the object created from the order information into the option functions for maximum efficiency and clarity. However, the restrictions of the project meant that we were not able to alter the function parameters. 
- *function executeOrder()* - Since there is a lot of overlap in terms of logic behind the 3 order types, we really only need one function to execute all of them. In the case of executeOrder, we follow a similar procedure to *Class Order*, in that we take an *orderType* parameter, and use it to determine whether to call 2 additional functions in the case we're dealing with a delivery or collection order. 

The entirety of the processing for these 3 options is done using a handful of SQL procedures and functions. Specifically;
- *insertOrder* - Creates a new order row in *orders* and returns the ID of the created row - note we use a sequence on this table to determine the new value of *orderID* to insert. 
- *insertOrderProduct* - A simple insert operation that uses the new *orderID* to create a linking row between the order and product. 
- *checkValidOrderProduct* - We want to ensure that we can actually fulfil the order before we commit the SQL to the database, so we perform a quantity check to determine if it's sufficient for a given product. If not, we throw an exception back to the calling Java function which will cancel and roll back the order.
Note that this is called on *trigger orderProductsTrigger*, specifically *BEFORE UPDATE OR INSERT*.
- *reduceStock* - A simple update procedure that lets us remove the sold stock from the inventory 
- *insertStaffOrder* - Another simple insert that links a staff member to our order.

In the case of option 2 and 3, we do all of the aforementioned inserts and checks, with the addition of 1 of the following 2:
- For option 2, we call *insertCollection* to create an entry in the collection table for the order
- For option 3, we call *insertDelivery* to create an entry in the delivery table for the order

### Option 4
Option 4 simply returns the result of a view, and prints it to the display. 

The view in question, *profitableProductsView* has a few notable features;
- In order to get the products that sold £0 of products, we utilised a *LEFT JOIN* in combination with *COALESCE()* to populate the table where *null* values were encountered
- The view is useful and reused in other contexts, such as in question 7 which we discuss later. 

### Option 5
The implementation of option 5 makes use of some interesting delete logic.
- We using a *DELETE USING* to join orders onto the collections table and delete all entries from orders where the collection is >= 8 days before the given date. 
- To calculate this deletion date we use an *INTERVAL* 
- After deleting, the *ON DELETE CASCADE* logic on the foreign keys we specified during table creation kicks in. This will delete all data in all tables that have a foreign key pointing at the deleted primary key in orders.
- We created a trigger *addUncollectedStock* that monitors the table *order_products*. When we delete a collections order, the trigger triggers a function that will calculate the new stock level and update the inventory.


### Option 6
Option 6 is another query that returns a table. The full query we use is *SELECT fullName, lifetimeSales FROM lifetimeSalesView*, where lifetimeSalesView is a view that we've defined in the schema. We then take the result set and pass it to a formatting function that we use to print tables, like in option 4. Some noteworthy points about the *lifetimeSalesView* view:
- We first work out the value of all sales in *orderProducts* which occurs in another view, *allSaleValues*
- We then sum these rows for each ID, to get a value of all each product sold
- From here, we can group by staffID, and sum the values of each product sold to get a value of products sold for that staff member
- We then make use of a *HAVING* clause on the aggregate function to ensure we're only returning staff with >= £50,000 in sales 
- The query is very similar to one we use in option 8

### Option 7
For this option, we make use of 2 queries - the main query simply returns a view that we've constructed, elegantly named *highestSellingProductSellersView*, and another which runs a *GROUP BY* and *COUNT* on the same view to tell us the order to display the data in.  

Although we don't do any filtering or ordering of the data directly in the java file, we do some interesting parsing of the data to convert it into the correct display format:
- Since our view returns multiple rows per staff member, each indicating the units of a particular high-selling product sold, we need to combine this vertical row count into a number of columns
- To do this, we use a small custom built class called a *Profile*. This profile stores all the products sold by a particular staff member. These products are stored in a HashMap, so that we can query in *O(1)* time when iterating over the list of profiles to see if they've sold the specific product. If the key query returns null, we populate the row with a 0. 
- We utilise our *profitableProductsView* from option 4 again here, this time filtering it for rows with > £20,000 value

### Option 8 
Option 8 uses a single SQL statement which is seperated into multiple views so that code can be reused and the query simplified. 

The main source of complexity for this query lies in two areas:
- The variable year input means we have to be able to take a year and only return data relevant to that year. We get around this issue by returning a table with data for all years as a view to the top level, and then filtering it with the year at the last possible step. The efficiency of returnin this entire table of all years rather than filtering at the base of the (complex) query is yet to be profiled and calculated, but we can be certain that it is sufficiently fast for this use case.
- Secondly, we need to check that the staff members have not just sold *a* product that has grossed £20,000 in the given year, but rather they have sold *one of each* of the products. There are multiple ways we could go about this, including methods utilising *EXISTS* or SQL functions that contain boolean checks and loops, but we have instead opted for a different method. We retrieve a list of the products that grossed £20,000 in a given year, and count them. We then retrieve a list of all the products that staff sold in a given year. We then *INNER JOIN* this with the aforementioned list of products, and count the results. We compare the two counts - the number of products that grossed £20,000 in a year, and the number of products the staff member sold in the year. Since the latter has been joined on the product list previously, we know that the only way the counts of these two lists can be the same is if every item in the product list is present in the staffs sales list. Thus, they are elegible for the aware (assuming they have also grossed £30,000 total). 


## Design Decisions

### Schema Choices
- We used a number of foreign key restraints and checks to ensure consistency across our database
- These include standard primary-foreign relationships, and cascading delete restraints

### Functions & Procedures 
- We utilise functions wherever possible to remove help with our procedures. Examples include *sufficientStock* used to check if we have adequate stock to to process an order before calling the procedure that will create a new table entry. 
- In all cases where an update or insert is needed, we do this via a procedure. This is because it is bad practice to have functions with side effects, despite the fact that Postgres technically allows it.  

### Triggers
Wherever possible, we try to perform updates using triggers. This automation of standard actions reduces the complexity of the code and means that a portion of our workload is handled automatically, which increases readability. 

### Rollbacks 
Wherever updates are performed, we utilise Postgres' built in rollbacks. It is good practice to use commits when performing large updates to a database. This is because in complex updates with multiple statements, a single error can invalidate a whole host of previously input data, which can then be difficult to remove. With commits, we can batch updates and then commit them together, or rollback to a previous state if one of the required updates fails. We utilis this heavily for the aforementioned reasons, particulary in options 1, 2 and 3

## Improvements
- Due to project restraints, we can't validate user input immediately - instead we must wait until the end of a menu cycle. This wastes users time if one of their inputs is rejected, especially in the case the rejected input was early in the menu or before a long list of products. We can fix this by not allowing the user to progress to the next option until their current option has been accepted as valid.
- There is the possibility of making some views slightly more generic and thus being able to reuse them more than once. An example of this would be *yearly30kStaff* used in option 6 and the *lifetimeSalesView* used in option 8. The only difference here is that one is interested in staff with > £30,000 sales and the other with > £50,000. There are undoubtedly similar examples of views that could have minor alterations made to make them reusable in other contexts and thus cut down on code duplication. 
- In 7, we use an auxiliary query to return the order of the data that we should use. Although it would make the main view more complex, this secondary query could be combined into the main query to reduce the amount of code we need, and contain all of the logic into a single query. 
- For Option 7, we could remove the requirement for any data processing in our java file by either a) utilising *crosstab* or b) utilising a crossjoin. The former would be less complex but goes about the project specification. This would cut down on our java code considerably, although arguable decrease readability.
- Implement the orderType as an enum rather than a string

## Testing ##
### Testing 1, 2 & 3

Options 1, 2 and 3 share a fairly significant amount of code, and thus the amount of testing needed is reduced. 
For example, the menu system is shared among all 3 options, as well as the execution method. The process for testing these two methods was as follow:
- After ensuring that when we provide the system with valid data, it performs the required updates, the problem shifts from logic to input. We need to make sure the input is always in a form that we are expecting.
- This is done with heavily exception handling on the *handleInput* method. We then ensured that this was valid via manual testing, including edge cases.
- We discovered during this testing that certain invalid inputs are accepted as valid - e.g. a negative stock amount, or a delivery date that happens before an order. We identified this and applied suitable fixes.

### Testing 4
- Ensure that all product ID's are shown in the table, whether any have been sold or not
- Check total value sold for each product and determine if it's correct. Add multiple orders of different types and check
- Ensure multiple products sold within a single order are reflected in the output. 

### Testing 5
- Create a set of orders and related entries in collections that have not been collected. Activate option 5 and enter a date 8 days after the collection data.
- Check all relevant data has been deleted from the associated tables
- Repeat step 1 but add a completed collection. Ensure this doesn't get deleted.

### Testing 6
- Test with 1 item in one order to ensure staff name displays only when order reaches £50,000 in value. 
- Test again with a single item across multiple orders to ensure it still appears at £50,000

### Testing 7 
- Test the component views on their own by inserting different data sets, predicting what they should return based on the logic of the question, and then run the view and check that the data matches expectations. 
- Build up from the smallest views into the larger sections, testing using the method in step 1
- Test the final view, which incorporates all of the other views and ensure that the results are as expected

### Testing 8
- Add a single order with one product, and set the value of that order to £20,000. Ensure it does not show up for the year when 8 is used.
- Repeat with a value of £30,000. Ensure this does get returned when 8 is used. 
- Use the two steps above so that we have 1 order of 1 product worth £20,000 and 1 order of 1 product worth £30,000. Ensure that the user is returned.
- Repeat the above but with different staff for each order. Ensure no results. 
- Change the year of the £20,000 order to the year before. Check that no results show for this year, but the £30,000 result shows for the current year.
- Add 4 orders of 1 product each, each worth £20,000. Assign to one staff and ensure staff member is displayed.
- Change 2 of the orders to another staff member and ensure both of them show up
- Split one of the 2 orders by a staff member to a different year and check that the staff member does not show up for that year or the current year, but the other shows up for the current year. 







