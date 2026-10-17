/* ---------- extra mock catalogue: 100 restaurants, 100 cuisines, 500 dishes ----------
   Loaded before app.js. Each row: [name, cuisines[], rating, votes, eta, costForTwo, emoji, offers, dishes]
   and each dish: [name, price, veg (1/0), emoji, category]. The first cuisine of a row is that
   restaurant's own cuisine (all 100 are distinct and become cuisine-strip entries); the first dish is the
   bestseller. A restaurant is "pure veg" only when every dish is vegetarian. Demo data only. */
const EXTRA_ROWS = [
  ['Seoul Street Kitchen', ['Korean', 'Asian'], 4.5, '4K+', 30, 700, '🍲', 1, [
    ['Chicken Bibimbap', 289, 0, '🍚', 'Mains'], ['Veg Kimchi Fried Rice', 229, 1, '🍚', 'Mains'], ['Korean Fried Chicken', 329, 0, '🍗', 'Starters'], ['Tteokbokki', 199, 1, '🍢', 'Snacks'], ['Banana Milk', 99, 1, '🥛', 'Drinks']]],
  ['Saigon Pho House', ['Vietnamese', 'Asian'], 4.4, '2K+', 34, 650, '🍜', 0, [
    ['Chicken Pho', 279, 0, '🍜', 'Noodles'], ['Veg Pho', 239, 1, '🍜', 'Noodles'], ['Banh Mi Sandwich', 189, 0, '🥖', 'Sandwiches'], ['Fresh Summer Rolls', 169, 1, '🌯', 'Starters'], ['Vietnamese Iced Coffee', 119, 1, '☕', 'Drinks']]],
  ['Beirut Nights', ['Lebanese', 'Middle Eastern'], 4.6, '5K+', 33, 800, '🧆', 1, [
    ['Mixed Mezze Platter', 399, 1, '🧆', 'Starters'], ['Chicken Shawarma Plate', 329, 0, '🌯', 'Mains'], ['Hummus with Pita', 179, 1, '🫓', 'Sides'], ['Falafel Wrap', 169, 1, '🌯', 'Wraps'], ['Baklava', 129, 1, '🍯', 'Desserts']]],
  ['Istanbul Grill', ['Turkish', 'Kebabs'], 4.3, '2K+', 36, 900, '🥙', 0, [
    ['Adana Kebab', 379, 0, '🍢', 'Grills'], ['Iskender Doner', 349, 0, '🥙', 'Mains'], ['Lahmacun', 229, 0, '🫓', 'Breads'], ['Veg Turkish Pide', 259, 1, '🥖', 'Breads'], ['Turkish Delight', 119, 1, '🍬', 'Desserts']]],
  ['Olive and Feta Taverna', ['Greek', 'Mediterranean'], 4.5, '3K+', 35, 950, '🫒', 1, [
    ['Greek Salad', 249, 1, '🥗', 'Salads'], ['Chicken Souvlaki', 339, 0, '🍢', 'Grills'], ['Moussaka', 319, 1, '🍆', 'Mains'], ['Spanakopita', 199, 1, '🥧', 'Starters'], ['Greek Yogurt with Honey', 129, 1, '🍯', 'Desserts']]],
  ['Casa Tapas', ['Spanish', 'Mediterranean'], 4.4, '2K+', 38, 1200, '🥘', 1, [
    ['Paella Valenciana', 449, 0, '🥘', 'Mains'], ['Veg Paella', 349, 1, '🥘', 'Mains'], ['Patatas Bravas', 199, 1, '🥔', 'Tapas'], ['Gambas al Ajillo', 399, 0, '🍤', 'Tapas'], ['Churros with Chocolate', 159, 1, '🍫', 'Desserts']]],
  ['Le Petit Bistro', ['French', 'Continental'], 4.6, '1K+', 40, 1400, '🥐', 1, [
    ['French Onion Soup', 249, 1, '🍲', 'Starters'], ['Coq au Vin', 449, 0, '🍗', 'Mains'], ['Ratatouille', 329, 1, '🍆', 'Mains'], ['Croque Monsieur', 289, 0, '🥪', 'Sandwiches'], ['Creme Brulee', 199, 1, '🍮', 'Desserts']]],
  ['Bavarian Haus', ['German', 'Continental'], 4.2, '1K+', 37, 1000, '🥨', 0, [
    ['Bratwurst Platter', 369, 0, '🌭', 'Mains'], ['Chicken Schnitzel', 339, 0, '🍗', 'Mains'], ['Soft Pretzel', 129, 1, '🥨', 'Snacks'], ['Potato Salad', 149, 1, '🥔', 'Sides'], ['Apple Strudel', 169, 1, '🍎', 'Desserts']]],
  ['Sunny Med Bowls', ['Mediterranean', 'Healthy'], 4.5, '3K+', 27, 650, '🥗', 1, [
    ['Falafel Quinoa Bowl', 259, 1, '🥗', 'Bowls'], ['Grilled Chicken Bowl', 299, 0, '🍗', 'Bowls'], ['Tabbouleh', 169, 1, '🥗', 'Salads'], ['Pita Chips and Hummus', 149, 1, '🫓', 'Sides'], ['Mint Lemonade', 99, 1, '🍋', 'Drinks']]],
  ['Sahara Platters', ['Middle Eastern', 'Mughlai'], 4.3, '2K+', 32, 750, '🍖', 0, [
    ['Lamb Mandi', 449, 0, '🍖', 'Mains'], ['Chicken Shish Taouk', 329, 0, '🍢', 'Grills'], ['Baba Ganoush', 179, 1, '🍆', 'Sides'], ['Labneh with Zaatar', 159, 1, '🥛', 'Sides'], ['Kunafa', 189, 1, '🍯', 'Desserts']]],
  ['Tehran Tales', ['Persian', 'Kebabs'], 4.4, '1K+', 39, 850, '🍚', 0, [
    ['Chelo Kebab', 399, 0, '🍢', 'Mains'], ['Zereshk Polo with Chicken', 369, 0, '🍚', 'Mains'], ['Kashke Bademjan', 189, 1, '🍆', 'Starters'], ['Mast-o-Khiar', 99, 1, '🥛', 'Sides'], ['Saffron Ice Cream', 139, 1, '🍦', 'Desserts']]],
  ['Addis Table', ['Ethiopian', 'African'], 4.3, '800+', 41, 800, '🍛', 0, [
    ['Doro Wat with Injera', 349, 0, '🍗', 'Mains'], ['Veg Combo Platter', 299, 1, '🥘', 'Mains'], ['Shiro Wat', 219, 1, '🥣', 'Mains'], ['Injera Basket', 79, 1, '🫓', 'Breads'], ['Ethiopian Spiced Tea', 89, 1, '🍵', 'Drinks']]],
  ['Marrakesh Souk', ['Moroccan', 'Mediterranean'], 4.5, '1K+', 37, 900, '🫔', 1, [
    ['Chicken Tagine', 369, 0, '🍲', 'Mains'], ['Veg Couscous', 269, 1, '🍚', 'Mains'], ['Harira Soup', 149, 1, '🍲', 'Starters'], ['Moroccan Flatbread', 89, 1, '🫓', 'Breads'], ['Mint Tea', 79, 1, '🍵', 'Drinks']]],
  ['Rio Churrasco', ['Brazilian', 'Grill'], 4.6, '2K+', 40, 1300, '🥩', 1, [
    ['Picanha Skewers', 449, 0, '🥩', 'Grills'], ['Chicken Heart Skewers', 299, 0, '🍢', 'Grills'], ['Pao de Queijo', 149, 1, '🧀', 'Snacks'], ['Feijoada Bowl', 329, 0, '🍲', 'Mains'], ['Brigadeiro', 109, 1, '🍫', 'Desserts']]],
  ['Lima Ceviche Bar', ['Peruvian', 'Seafood'], 4.4, '900+', 38, 1100, '🐟', 0, [
    ['Fish Ceviche', 399, 0, '🐟', 'Starters'], ['Lomo Saltado', 389, 0, '🥩', 'Mains'], ['Veg Causa', 249, 1, '🥔', 'Starters'], ['Yuca Fries', 149, 1, '🍟', 'Sides'], ['Fresh Lemonade', 119, 1, '🍋', 'Drinks']]],
  ['Pampas Parrilla', ['Argentine', 'Steakhouse'], 4.5, '1K+', 44, 1500, '🥩', 1, [
    ['Ribeye Steak', 599, 0, '🥩', 'Grills'], ['Chimichurri Chicken', 349, 0, '🍗', 'Grills'], ['Beef Empanadas (2 pcs)', 199, 0, '🥟', 'Starters'], ['Veg Empanadas (2 pcs)', 179, 1, '🥟', 'Starters'], ['Dulce de Leche Flan', 149, 1, '🍮', 'Desserts']]],
  ['Bayou Spice', ['Cajun', 'American'], 4.2, '1K+', 34, 850, '🦐', 0, [
    ['Jambalaya', 349, 0, '🍚', 'Mains'], ['Cajun Chicken Wrap', 249, 0, '🌯', 'Wraps'], ['Hush Puppies', 149, 1, '🍘', 'Sides'], ['Blackened Fish', 399, 0, '🐟', 'Mains'], ['Pecan Pie', 159, 1, '🥧', 'Desserts']]],
  ['Smokehouse 66', ['Southern BBQ', 'American'], 4.5, '3K+', 36, 1000, '🍖', 1, [
    ['Pulled Chicken Sandwich', 299, 0, '🥪', 'Sandwiches'], ['BBQ Chicken Ribs', 429, 0, '🍖', 'Grills'], ['Mac and Cheese', 199, 1, '🧀', 'Sides'], ['Coleslaw', 79, 1, '🥗', 'Sides'], ['Peach Cobbler', 169, 1, '🍑', 'Desserts']]],
  ['Aloha Poke Shack', ['Hawaiian', 'Healthy'], 4.4, '1K+', 28, 800, '🍍', 1, [
    ['Ahi Tuna Poke Bowl', 399, 0, '🍣', 'Bowls'], ['Tofu Poke Bowl', 279, 1, '🥗', 'Bowls'], ['Chicken Musubi', 149, 0, '🍙', 'Snacks'], ['Pineapple Fried Rice', 229, 1, '🍍', 'Mains'], ['Coconut Refresher', 109, 1, '🥥', 'Drinks']]],
  ['Bali Warung', ['Indonesian', 'Asian'], 4.3, '1K+', 33, 600, '🍢', 0, [
    ['Nasi Goreng', 249, 0, '🍚', 'Mains'], ['Veg Gado-Gado', 219, 1, '🥗', 'Mains'], ['Chicken Satay', 229, 0, '🍢', 'Starters'], ['Tempeh Skewers', 179, 1, '🍢', 'Starters'], ['Es Teler', 129, 1, '🥥', 'Desserts']]],
  ['Penang Hawker', ['Malaysian', 'Asian'], 4.4, '2K+', 31, 650, '🍛', 1, [
    ['Char Kway Teow', 269, 0, '🍜', 'Noodles'], ['Chicken Laksa', 289, 0, '🍜', 'Noodles'], ['Roti Canai', 119, 1, '🫓', 'Breads'], ['Veg Curry Mee', 239, 1, '🍜', 'Noodles'], ['Teh Tarik', 89, 1, '🍵', 'Drinks']]],
  ['Lion City Kitchen', ['Singaporean', 'Asian'], 4.5, '2K+', 30, 700, '🍗', 0, [
    ['Hainanese Chicken Rice', 299, 0, '🍗', 'Mains'], ['Prawn Hokkien Mee', 329, 0, '🍜', 'Noodles'], ['Veg Singapore Noodles', 219, 1, '🍜', 'Noodles'], ['Kaya Toast', 129, 1, '🍞', 'Snacks'], ['Barley Water', 79, 1, '🥤', 'Drinks']]],
  ['Manila Kitchen', ['Filipino', 'Asian'], 4.2, '700+', 35, 650, '🍗', 0, [
    ['Chicken Adobo', 279, 0, '🍗', 'Mains'], ['Pancit Canton', 229, 0, '🍜', 'Noodles'], ['Veg Lumpia (6 pcs)', 169, 1, '🌯', 'Starters'], ['Sisig Rice Bowl', 299, 0, '🍚', 'Bowls'], ['Halo-Halo', 149, 1, '🍧', 'Desserts']]],
  ['Yangon Khow Suey', ['Burmese', 'Asian'], 4.3, '900+', 32, 600, '🍲', 0, [
    ['Chicken Khow Suey', 269, 0, '🍜', 'Mains'], ['Veg Khow Suey', 229, 1, '🍜', 'Mains'], ['Samosa Salad', 169, 1, '🥗', 'Starters'], ['Coconut Rice', 119, 1, '🍚', 'Sides'], ['Falooda', 129, 1, '🥤', 'Desserts']]],
  ['Colombo Curry House', ['Sri Lankan', 'South Indian'], 4.4, '1K+', 34, 650, '🍛', 1, [
    ['Egg Hoppers (2 pcs)', 129, 0, '🥞', 'Breakfast'], ['Chicken Kottu Roti', 279, 0, '🍛', 'Mains'], ['Veg Kottu Roti', 229, 1, '🍛', 'Mains'], ['Jaffna Crab Curry', 449, 0, '🦀', 'Mains'], ['Watalappan', 119, 1, '🍮', 'Desserts']]],
  ['Himalayan Thali', ['Nepalese', 'Thali'], 4.5, '2K+', 29, 450, '🍱', 1, [
    ['Nepali Veg Thali', 249, 1, '🍱', 'Thalis'], ['Nepali Chicken Thali', 299, 0, '🍱', 'Thalis'], ['Aloo Achar', 99, 1, '🥔', 'Sides'], ['Sel Roti', 89, 1, '🍩', 'Breads'], ['Masala Chiya', 59, 1, '🍵', 'Drinks']]],
  ['Lhasa Thukpa Corner', ['Tibetan', 'Momos'], 4.3, '2K+', 24, 350, '🍜', 0, [
    ['Chicken Thukpa', 189, 0, '🍜', 'Soups'], ['Veg Thukpa', 169, 1, '🍜', 'Soups'], ['Tingmo with Curry', 159, 1, '🍞', 'Breads'], ['Chicken Shaphale', 179, 0, '🥟', 'Snacks'], ['Butter Tea', 79, 1, '🍵', 'Drinks']]],
  ['Kolkata Kitchen', ['Bengali', 'Seafood'], 4.6, '5K+', 36, 700, '🐟', 1, [
    ['Hilsa Paturi', 449, 0, '🐟', 'Mains'], ['Kosha Mangsho', 349, 0, '🍖', 'Mains'], ['Shukto', 189, 1, '🥬', 'Mains'], ['Luchi with Aloo Dum', 149, 1, '🫓', 'Breads'], ['Mishti Doi', 79, 1, '🍮', 'Desserts']]],
  ['Odisha Bhoj', ['Odia', 'Thali'], 4.3, '900+', 33, 450, '🍚', 0, [
    ['Dalma', 189, 1, '🥣', 'Mains'], ['Chicken Kassa', 299, 0, '🍗', 'Mains'], ['Pakhala Bhata', 149, 1, '🍚', 'Rice'], ['Mixed Veg Thali', 229, 1, '🍱', 'Thalis'], ['Chhena Poda', 119, 1, '🍰', 'Desserts']]],
  ['Brahmaputra Table', ['Assamese', 'Thali'], 4.4, '700+', 37, 550, '🍚', 0, [
    ['Masor Tenga', 299, 0, '🐟', 'Mains'], ['Khar with Rice', 189, 1, '🍚', 'Mains'], ['Assam Chicken Curry', 279, 0, '🍗', 'Mains'], ['Til Pitha', 99, 1, '🍘', 'Snacks'], ['Assam Tea', 69, 1, '🍵', 'Drinks']]],
  ['Malabar Coast', ['Kerala', 'South Indian'], 4.6, '6K+', 32, 600, '🥥', 1, [
    ['Appam with Veg Stew', 189, 1, '🥞', 'Breakfast'], ['Kerala Fish Curry Meals', 299, 0, '🐟', 'Meals'], ['Kerala Parotta (2 pcs)', 89, 1, '🫓', 'Breads'], ['Nadan Chicken Roast', 279, 0, '🍗', 'Mains'], ['Palada Payasam', 99, 1, '🍮', 'Desserts']]],
  ['Karaikudi Spice', ['Chettinad', 'South Indian'], 4.5, '3K+', 35, 650, '🌶️', 0, [
    ['Chettinad Chicken Curry', 299, 0, '🍗', 'Mains'], ['Veg Kuzhambu Meals', 199, 1, '🍚', 'Meals'], ['Mutton Chukka', 349, 0, '🍖', 'Starters'], ['Kuzhi Paniyaram', 129, 1, '🥞', 'Snacks'], ['Filter Coffee', 59, 1, '☕', 'Drinks']]],
  ['Nizam Dastarkhwan', ['Hyderabadi', 'Mughlai'], 4.6, '9K+', 38, 700, '🍲', 1, [
    ['Hyderabadi Haleem', 249, 0, '🍲', 'Mains'], ['Hyderabadi Veg Dum', 229, 1, '🥘', 'Mains'], ['Lukhmi (4 pcs)', 149, 0, '🥟', 'Starters'], ['Khubani Ka Meetha', 129, 1, '🍑', 'Desserts'], ['Irani Chai', 59, 1, '🍵', 'Drinks']]],
  ['Lucknowi Daawat', ['Awadhi', 'Mughlai'], 4.5, '4K+', 40, 900, '🍖', 1, [
    ['Galouti Kebab', 329, 0, '🍢', 'Starters'], ['Awadhi Mutton Korma', 379, 0, '🍖', 'Mains'], ['Dum Paneer Pasanda', 279, 1, '🧀', 'Mains'], ['Sheermal', 79, 1, '🫓', 'Breads'], ['Shahi Tukda', 129, 1, '🍞', 'Desserts']]],
  ['Dal Lake Wazwan', ['Kashmiri', 'Mughlai'], 4.4, '2K+', 42, 950, '🍖', 0, [
    ['Rogan Josh', 379, 0, '🍖', 'Mains'], ['Dum Aloo Kashmiri', 229, 1, '🥔', 'Mains'], ['Kashmiri Pulao', 259, 1, '🍚', 'Rice'], ['Girda Bread', 49, 1, '🫓', 'Breads'], ['Noon Chai', 79, 1, '🍵', 'Drinks']]],
  ['Amritsari Dhaba', ['Punjabi', 'North Indian'], 4.5, '10K+', 31, 600, '🧈', 1, [
    ['Butter Chicken', 329, 0, '🍗', 'Mains'], ['Sarson Da Saag with Makki Roti', 249, 1, '🥬', 'Mains'], ['Amritsari Kulcha', 139, 1, '🫓', 'Breads'], ['Chole Bhature', 169, 1, '🍛', 'Mains'], ['Sweet Lassi', 89, 1, '🥛', 'Drinks']]],
  ['Panjim Shack', ['Goan', 'Seafood'], 4.4, '3K+', 35, 850, '🦀', 1, [
    ['Prawn Balchao', 379, 0, '🍤', 'Mains'], ['Goan Fish Curry Rice', 299, 0, '🐟', 'Mains'], ['Veg Xacuti', 249, 1, '🥘', 'Mains'], ['Poi Bread (2 pcs)', 59, 1, '🍞', 'Breads'], ['Bebinca', 149, 1, '🍰', 'Desserts']]],
  ['Konkan Katta', ['Konkani', 'Seafood'], 4.3, '1K+', 36, 650, '🐟', 0, [
    ['Bombil Fry', 279, 0, '🐟', 'Starters'], ['Chicken Sukka', 289, 0, '🍗', 'Mains'], ['Vaal Usal', 169, 1, '🥣', 'Mains'], ['Sol Kadhi', 79, 1, '🥛', 'Drinks'], ['Ukadiche Modak (3 pcs)', 129, 1, '🥟', 'Desserts']]],
  ['Mangaluru Meals', ['Mangalorean', 'Seafood'], 4.5, '2K+', 34, 700, '🦐', 1, [
    ['Chicken Ghee Roast', 299, 0, '🍗', 'Mains'], ['Neer Dosa (3 pcs)', 119, 1, '🥞', 'Breakfast'], ['Prawn Sukka', 349, 0, '🦐', 'Starters'], ['Mangalore Buns', 99, 1, '🍞', 'Snacks'], ['Tender Coconut Payasam', 109, 1, '🥥', 'Desserts']]],
  ['Patna Litti House', ['Bihari', 'Street Food'], 4.3, '1K+', 26, 350, '🫓', 0, [
    ['Litti Chokha (4 pcs)', 169, 1, '🫓', 'Mains'], ['Champaran Mutton', 349, 0, '🍖', 'Mains'], ['Sattu Paratha', 119, 1, '🫓', 'Breads'], ['Sattu Sharbat', 59, 1, '🥤', 'Drinks'], ['Thekua', 79, 1, '🍪', 'Desserts']]],
  ['Sindh Rasoi', ['Sindhi', 'North Indian'], 4.2, '800+', 30, 500, '🍛', 0, [
    ['Sai Bhaji', 189, 1, '🥬', 'Mains'], ['Sindhi Kadhi', 179, 1, '🥣', 'Mains'], ['Dal Pakwan', 159, 1, '🫓', 'Breakfast'], ['Koki', 79, 1, '🫓', 'Breads'], ['Seviyan', 99, 1, '🍮', 'Desserts']]],
  ['Irani Cafe Bombay', ['Parsi', 'Café'], 4.4, '3K+', 27, 500, '🥚', 1, [
    ['Berry Pulao', 299, 0, '🍚', 'Mains'], ['Chicken Dhansak', 329, 0, '🍲', 'Mains'], ['Akuri on Toast', 169, 0, '🍳', 'Breakfast'], ['Bun Maska', 59, 1, '🍞', 'Snacks'], ['Raspberry Soda', 69, 1, '🥤', 'Drinks']]],
  ['Shahi Darbar', ['Mughlai', 'North Indian'], 4.5, '7K+', 37, 800, '🍖', 1, [
    ['Chicken Mughlai Korma', 319, 0, '🍗', 'Mains'], ['Paneer Lababdar', 269, 1, '🧀', 'Mains'], ['Mutton Seekh Kebab', 349, 0, '🍢', 'Starters'], ['Rumali Roti', 39, 1, '🫓', 'Breads'], ['Phirni', 99, 1, '🍮', 'Desserts']]],
  ['Tandoor Station', ['Tandoor', 'North Indian'], 4.4, '5K+', 29, 650, '🍢', 0, [
    ['Tandoori Chicken (Half)', 289, 0, '🍗', 'Grills'], ['Paneer Tikka', 249, 1, '🧀', 'Grills'], ['Tandoori Soya Chaap', 219, 1, '🍢', 'Grills'], ['Butter Naan', 59, 1, '🫓', 'Breads'], ['Masala Chaas', 59, 1, '🥛', 'Drinks']]],
  ['Seekh and Sheekh', ['Kebabs', 'Mughlai'], 4.3, '2K+', 28, 600, '🍢', 1, [
    ['Chicken Seekh Kebab', 249, 0, '🍢', 'Grills'], ['Hara Bhara Kebab', 199, 1, '🥬', 'Grills'], ['Mutton Galouti', 339, 0, '🍖', 'Grills'], ['Veg Shammi Kebab', 189, 1, '🍢', 'Grills'], ['Lacha Paratha', 69, 1, '🫓', 'Breads']]],
  ['Chowpatty Chaat Corner', ['Chaat', 'Street Food'], 4.5, '8K+', 19, 250, '🥗', 1, [
    ['Papdi Chaat', 99, 1, '🥗', 'Chaat'], ['Dahi Puri', 89, 1, '🥙', 'Chaat'], ['Sev Puri', 79, 1, '🥙', 'Chaat'], ['Raj Kachori', 119, 1, '🥙', 'Chaat'], ['Jaljeera', 49, 1, '🥤', 'Drinks']]],
  ['Juhu Pav Bhaji', ['Pav Bhaji', 'Street Food'], 4.6, '9K+', 21, 300, '🍞', 1, [
    ['Butter Pav Bhaji', 159, 1, '🍞', 'Mains'], ['Cheese Pav Bhaji', 189, 1, '🧀', 'Mains'], ['Masala Pav', 89, 1, '🍞', 'Snacks'], ['Extra Pav (2 pcs)', 30, 1, '🍞', 'Sides'], ['Sweet Lassi', 79, 1, '🥛', 'Drinks']]],
  ['Mumbai Vada Pav Co.', ['Vada Pav', 'Street Food'], 4.4, '6K+', 15, 150, '🍔', 0, [
    ['Classic Vada Pav', 39, 1, '🍔', 'Snacks'], ['Cheese Vada Pav', 59, 1, '🍔', 'Snacks'], ['Samosa Pav', 49, 1, '🥟', 'Snacks'], ['Misal Pav', 119, 1, '🍛', 'Mains'], ['Cutting Chai', 29, 1, '🍵', 'Drinks']]],
  ['The Sandwich Works', ['Sandwiches', 'Café'], 4.2, '3K+', 20, 350, '🥪', 1, [
    ['Grilled Veg Sandwich', 119, 1, '🥪', 'Sandwiches'], ['Chicken Club Sandwich', 189, 0, '🥪', 'Sandwiches'], ['Paneer Tikka Sandwich', 149, 1, '🥪', 'Sandwiches'], ['Peri Peri Fries', 99, 1, '🍟', 'Sides'], ['Iced Lemon Tea', 79, 1, '🧋', 'Drinks']]],
  ['Shawarma Stop', ['Shawarma', 'Middle Eastern'], 4.3, '5K+', 18, 300, '🌯', 0, [
    ['Chicken Shawarma Roll', 129, 0, '🌯', 'Rolls'], ['Paneer Shawarma Roll', 119, 1, '🌯', 'Rolls'], ['Shawarma Platter', 249, 0, '🍽️', 'Mains'], ['Garlic Fries', 99, 1, '🍟', 'Sides'], ['Cola', 50, 1, '🥤', 'Drinks']]],
  ['Falafel Factory', ['Falafel', 'Middle Eastern'], 4.2, '2K+', 22, 350, '🧆', 1, [
    ['Falafel Pita', 139, 1, '🧆', 'Wraps'], ['Falafel Bowl', 189, 1, '🥗', 'Bowls'], ['Hummus Plate', 149, 1, '🫓', 'Sides'], ['Pickled Veg Side', 69, 1, '🥒', 'Sides'], ['Fresh Mint Lemonade', 89, 1, '🍋', 'Drinks']]],
  ['Dog House Deli', ['Hot Dogs', 'American'], 4.1, '2K+', 21, 350, '🌭', 0, [
    ['Classic Hot Dog', 139, 0, '🌭', 'Hot Dogs'], ['Chicken Cheese Dog', 169, 0, '🌭', 'Hot Dogs'], ['Veg Garden Dog', 129, 1, '🌭', 'Hot Dogs'], ['Chilli Cheese Fries', 149, 1, '🍟', 'Sides'], ['Chocolate Shake', 129, 1, '🥤', 'Drinks']]],
  ['Crunch Bucket', ['Fried Chicken', 'Fast Food'], 4.3, '7K+', 23, 500, '🍗', 1, [
    ['Fried Chicken Bucket (6 pcs)', 449, 0, '🍗', 'Buckets'], ['Zinger Burger', 169, 0, '🍔', 'Burgers'], ['Popcorn Chicken', 149, 0, '🍗', 'Snacks'], ['Corn on the Cob', 79, 1, '🌽', 'Sides'], ['Cola', 60, 1, '🥤', 'Drinks']]],
  ['Wing Republic', ['Wings', 'American'], 4.4, '4K+', 25, 500, '🍗', 1, [
    ['Buffalo Wings (8 pcs)', 289, 0, '🍗', 'Wings'], ['Honey Garlic Wings (8 pcs)', 299, 0, '🍗', 'Wings'], ['BBQ Wings (8 pcs)', 299, 0, '🍗', 'Wings'], ['Cheesy Fries', 129, 1, '🍟', 'Sides'], ['Lemon Iced Tea', 89, 1, '🧋', 'Drinks']]],
  ['Prime Cut Steakhouse', ['Steakhouse', 'Continental'], 4.6, '2K+', 43, 1800, '🥩', 1, [
    ['Grilled Chicken Steak', 449, 0, '🥩', 'Grills'], ['Peppercorn Fish Steak', 499, 0, '🐟', 'Grills'], ['Grilled Veg Steak', 349, 1, '🥦', 'Grills'], ['Mashed Potato', 129, 1, '🥔', 'Sides'], ['Chocolate Fondant', 199, 1, '🍫', 'Desserts']]],
  ['Charcoal and Co', ['Grill', 'Barbecue'], 4.4, '3K+', 34, 900, '🔥', 0, [
    ['Grilled Chicken Platter', 339, 0, '🍗', 'Grills'], ['Grilled Paneer Steak', 289, 1, '🧀', 'Grills'], ['Corn Ribs', 159, 1, '🌽', 'Starters'], ['Garlic Bread', 119, 1, '🥖', 'Breads'], ['Sparkling Lemon', 99, 1, '🍋', 'Drinks']]],
  ['Tokyo Roll Studio', ['Sushi', 'Japanese'], 4.5, '3K+', 36, 1200, '🍣', 1, [
    ['Salmon Nigiri (4 pcs)', 449, 0, '🍣', 'Sushi'], ['California Roll (8 pcs)', 349, 0, '🍣', 'Sushi'], ['Avocado Cucumber Roll (8 pcs)', 299, 1, '🥑', 'Sushi'], ['Miso Soup', 129, 1, '🍲', 'Soups'], ['Matcha Mochi', 169, 1, '🍡', 'Desserts']]],
  ['Ramen Rebel', ['Ramen', 'Japanese'], 4.4, '2K+', 32, 800, '🍜', 0, [
    ['Chicken Tonkotsu-Style Ramen', 329, 0, '🍜', 'Ramen'], ['Miso Veg Ramen', 289, 1, '🍜', 'Ramen'], ['Chicken Gyoza (5 pcs)', 199, 0, '🥟', 'Starters'], ['Edamame', 129, 1, '🫛', 'Sides'], ['Ramune Soda', 119, 1, '🥤', 'Drinks']]],
  ['Dim Sum Diaries', ['Dim Sum', 'Chinese'], 4.5, '4K+', 31, 700, '🥟', 1, [
    ['Chicken Siu Mai (4 pcs)', 199, 0, '🥟', 'Dim Sum'], ['Veg Crystal Dumplings (4 pcs)', 179, 1, '🥟', 'Dim Sum'], ['Prawn Har Gow (4 pcs)', 229, 0, '🥟', 'Dim Sum'], ['Veg Spring Rolls', 149, 1, '🌯', 'Starters'], ['Jasmine Tea', 69, 1, '🍵', 'Drinks']]],
  ['Hot Pot Haven', ['Hot Pot', 'Asian'], 4.3, '1K+', 38, 1000, '🍲', 1, [
    ['Spicy Sichuan Hot Pot', 499, 0, '🍲', 'Hot Pots'], ['Mushroom Veg Hot Pot', 429, 1, '🍄', 'Hot Pots'], ['Noodle Add-on', 99, 1, '🍜', 'Sides'], ['Fish Ball Add-on', 149, 0, '🍢', 'Sides'], ['Plum Juice', 79, 1, '🍹', 'Drinks']]],
  ['Chengdu Chilli', ['Sichuan', 'Chinese'], 4.4, '3K+', 30, 650, '🌶️', 0, [
    ['Mapo Tofu', 249, 1, '🌶️', 'Mains'], ['Kung Pao Chicken', 289, 0, '🍗', 'Mains'], ['Dan Dan Noodles', 239, 0, '🍜', 'Noodles'], ['Dry Chilli Cauliflower', 219, 1, '🥦', 'Starters'], ['Iced Peach Tea', 89, 1, '🧋', 'Drinks']]],
  ['Canton Wok', ['Cantonese', 'Chinese'], 4.3, '2K+', 29, 600, '🥡', 0, [
    ['Sweet and Sour Chicken', 279, 0, '🍗', 'Mains'], ['Cantonese Veg Chow Mein', 239, 1, '🍜', 'Noodles'], ['Honey Chilli Potato', 189, 1, '🥔', 'Starters'], ['Steamed Fish Fillet', 389, 0, '🐟', 'Mains'], ['Lychee Cooler', 99, 1, '🍹', 'Drinks']]],
  ['Hakka Express', ['Indo-Chinese', 'Chinese'], 4.2, '9K+', 24, 400, '🥡', 1, [
    ['Veg Hakka Noodles', 179, 1, '🍜', 'Noodles'], ['Chilli Chicken Gravy', 249, 0, '🍗', 'Mains'], ['Gobi Manchurian', 189, 1, '🥦', 'Starters'], ['Schezwan Fried Rice', 199, 1, '🍚', 'Rice'], ['Sweet Corn Soup', 99, 1, '🌽', 'Soups']]],
  ['Asia Alley', ['Pan-Asian', 'Asian'], 4.4, '2K+', 33, 750, '🥢', 1, [
    ['Teriyaki Chicken Rice Bowl', 289, 0, '🍚', 'Bowls'], ['Veg Katsu Curry', 269, 1, '🍛', 'Mains'], ['Edamame Dumplings', 189, 1, '🥟', 'Starters'], ['Chicken Bao Buns (2 pcs)', 219, 0, '🥟', 'Starters'], ['Green Tea', 69, 1, '🍵', 'Drinks']]],
  ['Tex Mex Cantina', ['Tex-Mex', 'Mexican'], 4.3, '3K+', 30, 700, '🌯', 1, [
    ['Chicken Fajitas', 349, 0, '🌯', 'Mains'], ['Veg Quesadilla', 239, 1, '🫓', 'Mains'], ['Loaded Nachos', 249, 1, '🧀', 'Starters'], ['Mexican Rice', 119, 1, '🍚', 'Sides'], ['Horchata', 109, 1, '🥛', 'Drinks']]],
  ['Taco Libre', ['Tacos', 'Mexican'], 4.4, '4K+', 22, 450, '🌮', 1, [
    ['Chicken Tacos (3 pcs)', 249, 0, '🌮', 'Tacos'], ['Beans Tacos (3 pcs)', 219, 1, '🌮', 'Tacos'], ['Paneer Tikka Tacos (3 pcs)', 229, 1, '🌮', 'Tacos'], ['Guacamole and Chips', 179, 1, '🥑', 'Sides'], ['Churro Bites', 129, 1, '🍩', 'Desserts']]],
  ['Burrito Bros', ['Burritos', 'Mexican'], 4.2, '2K+', 23, 450, '🌯', 0, [
    ['Chicken Burrito', 269, 0, '🌯', 'Burritos'], ['Veg Burrito Bowl', 229, 1, '🥗', 'Bowls'], ['Cheese Burrito', 219, 1, '🌯', 'Burritos'], ['Refried Beans', 99, 1, '🫘', 'Sides'], ['Mango Agua Fresca', 99, 1, '🥭', 'Drinks']]],
  ['Nacho Average Joe', ['Nachos', 'Mexican'], 4.1, '1K+', 20, 400, '🧀', 1, [
    ['Classic Cheesy Nachos', 199, 1, '🧀', 'Nachos'], ['Chicken Loaded Nachos', 259, 0, '🍗', 'Nachos'], ['Jalapeno Poppers', 159, 1, '🌶️', 'Starters'], ['Salsa Trio', 119, 1, '🍅', 'Sides'], ['Lime Soda', 69, 1, '🍋', 'Drinks']]],
  ['Pasta Pronto', ['Pasta', 'Italian'], 4.4, '5K+', 26, 550, '🍝', 1, [
    ['Penne Arrabbiata', 229, 1, '🍝', 'Pasta'], ['Chicken Alfredo Fettuccine', 289, 0, '🍝', 'Pasta'], ['Lasagna Bolognese', 319, 0, '🍝', 'Baked'], ['Cheesy Garlic Bread', 139, 1, '🥖', 'Breads'], ['Tiramisu Cup', 149, 1, '🍮', 'Desserts']]],
  ['Gelato Giardino', ['Gelato', 'Desserts'], 4.7, '3K+', 20, 350, '🍨', 1, [
    ['Pistachio Gelato', 139, 1, '🍨', 'Gelato'], ['Hazelnut Gelato', 139, 1, '🍨', 'Gelato'], ['Affogato', 169, 1, '☕', 'Desserts'], ['Sorbet Trio', 179, 1, '🍧', 'Gelato'], ['Waffle Cone Sundae', 199, 1, '🍦', 'Sundaes']]],
  ['Golden Crust Bakery', ['Bakery', 'Café'], 4.5, '6K+', 19, 300, '🥐', 0, [
    ['Butter Croissant', 89, 1, '🥐', 'Bakes'], ['Chicken Puff', 69, 0, '🥧', 'Snacks'], ['Sourdough Loaf', 179, 1, '🍞', 'Breads'], ['Veg Patty', 59, 1, '🥟', 'Snacks'], ['Hot Chocolate', 119, 1, '☕', 'Drinks']]],
  ['Cake Atelier', ['Cakes', 'Desserts'], 4.6, '5K+', 28, 600, '🎂', 1, [
    ['Black Forest Pastry', 109, 1, '🍰', 'Pastries'], ['Red Velvet Slice', 139, 1, '🍰', 'Pastries'], ['Chocolate Truffle Cake (500 g)', 549, 1, '🎂', 'Cakes'], ['Fruit Cake Jar', 129, 1, '🥫', 'Desserts'], ['Cold Coffee', 119, 1, '🥤', 'Drinks']]],
  ['Dunk Donut Co.', ['Donuts', 'Desserts'], 4.3, '3K+', 19, 300, '🍩', 1, [
    ['Glazed Donut', 69, 1, '🍩', 'Donuts'], ['Choco Sprinkle Donut', 89, 1, '🍩', 'Donuts'], ['Boston Cream Donut', 99, 1, '🍩', 'Donuts'], ['Donut Box of 6', 449, 1, '🍩', 'Boxes'], ['Iced Coffee', 109, 1, '🧋', 'Drinks']]],
  ['Stack House', ['Pancakes', 'Café'], 4.5, '4K+', 24, 500, '🥞', 1, [
    ['Buttermilk Pancake Stack', 189, 1, '🥞', 'Pancakes'], ['Blueberry Pancakes', 219, 1, '🥞', 'Pancakes'], ['Chicken Sausage Pancake Stack', 249, 0, '🥞', 'Pancakes'], ['Banana Nutella Pancakes', 229, 1, '🍌', 'Pancakes'], ['Fresh Orange Juice', 99, 1, '🍊', 'Drinks']]],
  ['Crepe Corner', ['Crepes', 'Desserts'], 4.4, '2K+', 22, 400, '🥞', 0, [
    ['Nutella Banana Crepe', 169, 1, '🥞', 'Sweet Crepes'], ['Cheese Chicken Crepe', 189, 0, '🥞', 'Savoury Crepes'], ['Strawberry Cream Crepe', 179, 1, '🍓', 'Sweet Crepes'], ['Mushroom Spinach Crepe', 179, 1, '🍄', 'Savoury Crepes'], ['Hot Chocolate', 119, 1, '☕', 'Drinks']]],
  ['Sunday Brunch Club', ['Brunch', 'Café'], 4.6, '3K+', 31, 900, '🍳', 1, [
    ['Avocado Toast with Egg', 249, 0, '🥑', 'Brunch'], ['Eggs Benedict', 289, 0, '🍳', 'Brunch'], ['Veg Shakshuka', 229, 1, '🍅', 'Brunch'], ['Berry Granola Bowl', 199, 1, '🥣', 'Bowls'], ['Orange Spritz', 119, 1, '🍊', 'Drinks']]],
  ['Egg-cellent Eats', ['Egg Specials', 'Breakfast'], 4.2, '5K+', 17, 250, '🍳', 0, [
    ['Masala Omelette', 89, 0, '🍳', 'Eggs'], ['Egg Bhurji with Pav', 129, 0, '🍳', 'Eggs'], ['Egg Fried Rice', 159, 0, '🍚', 'Rice'], ['Egg Curry', 149, 0, '🥘', 'Mains'], ['Masala Chai', 39, 1, '🍵', 'Drinks']]],
  ['Blend and Bloom', ['Smoothies', 'Healthy'], 4.4, '3K+', 18, 350, '🥤', 1, [
    ['Mango Smoothie', 139, 1, '🥭', 'Smoothies'], ['Berry Blast Smoothie', 159, 1, '🫐', 'Smoothies'], ['Green Detox Smoothie', 169, 1, '🥬', 'Smoothies'], ['Peanut Butter Banana Smoothie', 179, 1, '🍌', 'Smoothies'], ['Chia Seed Bowl', 189, 1, '🥣', 'Desserts']]],
  ['Fresh Press Juice Bar', ['Juices', 'Healthy'], 4.3, '2K+', 16, 250, '🍹', 0, [
    ['Watermelon Juice', 99, 1, '🍉', 'Juices'], ['Sugarcane Juice', 59, 1, '🥤', 'Juices'], ['Pomegranate Juice', 149, 1, '🍹', 'Juices'], ['Carrot Ginger Shot', 79, 1, '🥕', 'Shots'], ['Mixed Fruit Platter', 129, 1, '🍇', 'Desserts']]],
  ['Boba Bay', ['Bubble Tea', 'Drinks'], 4.5, '4K+', 17, 350, '🧋', 1, [
    ['Classic Milk Tea Boba', 159, 1, '🧋', 'Drinks'], ['Taro Boba', 169, 1, '🧋', 'Drinks'], ['Matcha Boba', 179, 1, '🧋', 'Drinks'], ['Brown Sugar Boba', 189, 1, '🧋', 'Drinks'], ['Cheese Foam Tea', 199, 1, '🧋', 'Drinks']]],
  ['Shake Shack Lane', ['Milkshakes', 'Desserts'], 4.3, '3K+', 18, 350, '🥤', 1, [
    ['Oreo Milkshake', 149, 1, '🥤', 'Shakes'], ['KitKat Milkshake', 159, 1, '🥤', 'Shakes'], ['Belgian Chocolate Milkshake', 169, 1, '🥤', 'Shakes'], ['Strawberry Milkshake', 139, 1, '🍓', 'Shakes'], ['Fries', 99, 1, '🍟', 'Sides']]],
  ['Chai Tapri', ['Tea', 'Café'], 4.4, '8K+', 14, 150, '🍵', 0, [
    ['Kulhad Masala Chai', 49, 1, '🍵', 'Tea'], ['Irani Chai', 59, 1, '🍵', 'Tea'], ['Bun Maska', 69, 1, '🍞', 'Snacks'], ['Samosa (2 pcs)', 59, 1, '🥟', 'Snacks'], ['Lemon Tea', 49, 1, '🍋', 'Tea']]],
  ['Bean Theory Roasters', ['Coffee Roasters', 'Café'], 4.6, '5K+', 21, 450, '☕', 1, [
    ['Filter Coffee', 69, 1, '☕', 'Coffee'], ['Flat White', 159, 1, '☕', 'Coffee'], ['Cold Brew', 179, 1, '🧊', 'Coffee'], ['Almond Croissant', 149, 1, '🥐', 'Bakes'], ['Banana Bread Slice', 119, 1, '🍞', 'Bakes']]],
  ['Punjab Lassi Wala', ['Lassi and Chaas', 'Drinks'], 4.5, '6K+', 15, 200, '🥛', 0, [
    ['Sweet Lassi', 79, 1, '🥛', 'Drinks'], ['Mango Lassi', 99, 1, '🥭', 'Drinks'], ['Rose Lassi', 99, 1, '🌹', 'Drinks'], ['Masala Chaas', 49, 1, '🥛', 'Drinks'], ['Aloo Paratha', 99, 1, '🫓', 'Breads']]],
  ['Kulfi Kings', ['Kulfi', 'Desserts'], 4.6, '4K+', 17, 250, '🍦', 1, [
    ['Malai Kulfi', 69, 1, '🍦', 'Kulfi'], ['Pista Kulfi', 89, 1, '🍦', 'Kulfi'], ['Kulfi Falooda', 129, 1, '🍨', 'Desserts'], ['Mango Kulfi', 79, 1, '🥭', 'Kulfi'], ['Rose Falooda', 119, 1, '🥤', 'Desserts']]],
  ['Mithaiwala Sweets', ['Mithai', 'Desserts'], 4.7, '10K+', 25, 450, '🍬', 1, [
    ['Kaju Katli (250 g)', 299, 1, '🍬', 'Sweets'], ['Motichoor Ladoo (250 g)', 189, 1, '🟠', 'Sweets'], ['Rasgulla (6 pcs)', 169, 1, '⚪', 'Sweets'], ['Gulab Jamun (6 pcs)', 159, 1, '🟤', 'Sweets'], ['Soan Papdi (250 g)', 149, 1, '🍥', 'Sweets']]],
  ['Toss and Go Salads', ['Salad Bar', 'Healthy'], 4.4, '2K+', 19, 450, '🥗', 1, [
    ['Caesar Chicken Salad', 249, 0, '🥗', 'Salads'], ['Greek Feta Salad', 229, 1, '🥗', 'Salads'], ['Quinoa Corn Salad', 219, 1, '🥗', 'Salads'], ['Fruit Chaat Bowl', 149, 1, '🍇', 'Desserts'], ['Lemon Mint Cooler', 89, 1, '🍋', 'Drinks']]],
  ['Plant Kind Kitchen', ['Vegan', 'Healthy'], 4.5, '2K+', 27, 650, '🌱', 1, [
    ['Vegan Buddha Bowl', 279, 1, '🥗', 'Bowls'], ['Jackfruit Tacos', 249, 1, '🌮', 'Mains'], ['Cashew Cheese Pizza', 299, 1, '🍕', 'Mains'], ['Oat Milk Latte', 149, 1, '☕', 'Drinks'], ['Vegan Brownie', 139, 1, '🍫', 'Desserts']]],
  ['Keto Kitchen', ['Keto', 'Healthy'], 4.3, '1K+', 29, 700, '🥑', 0, [
    ['Grilled Chicken with Zucchini Noodles', 329, 0, '🍗', 'Mains'], ['Paneer Tikka Salad', 249, 1, '🥗', 'Salads'], ['Cauliflower Fried Rice', 229, 1, '🥦', 'Mains'], ['Almond Flour Wrap', 199, 1, '🌯', 'Wraps'], ['Keto Chocolate Mousse', 169, 1, '🍫', 'Desserts']]],
  ['Gym Fuel Bowls', ['Protein Bowls', 'Healthy'], 4.4, '3K+', 23, 500, '💪', 1, [
    ['Chicken Rice Protein Bowl', 289, 0, '🍗', 'Bowls'], ['Tofu Quinoa Bowl', 269, 1, '🥗', 'Bowls'], ['Egg White Omelette', 149, 0, '🍳', 'Eggs'], ['Peanut Butter Protein Shake', 179, 1, '🥤', 'Drinks'], ['Sprouts Salad', 129, 1, '🌱', 'Salads']]],
  ['Jain Bhojanalay', ['Jain', 'Gujarati'], 4.5, '4K+', 30, 450, '🥬', 1, [
    ['Jain Pav Bhaji', 169, 1, '🍞', 'Mains'], ['Jain Veg Thali', 249, 1, '🍱', 'Thalis'], ['Jain Chole Kulcha', 179, 1, '🫓', 'Mains'], ['Sabudana Khichdi', 129, 1, '🍚', 'Snacks'], ['Mohanthal', 109, 1, '🍬', 'Desserts']]],
  ['Satvik Sutra', ['Satvik', 'Healthy'], 4.4, '1K+', 28, 450, '🍃', 0, [
    ['Khichdi with Ghee', 169, 1, '🍚', 'Mains'], ['Satvik Thali', 249, 1, '🍱', 'Thalis'], ['Moong Dal Chilla', 129, 1, '🥞', 'Breakfast'], ['Seasonal Fruit Bowl', 119, 1, '🍎', 'Desserts'], ['Tulsi Kadha', 69, 1, '🍵', 'Drinks']]],
  ['Millet Mantra', ['Millet', 'Healthy'], 4.3, '1K+', 26, 400, '🌾', 1, [
    ['Ragi Dosa', 119, 1, '🥞', 'Breakfast'], ['Jowar Roti Thali', 229, 1, '🍱', 'Thalis'], ['Bajra Khichdi', 169, 1, '🍚', 'Mains'], ['Millet Upma', 109, 1, '🥣', 'Breakfast'], ['Ragi Malt', 69, 1, '🥛', 'Drinks']]],
  ['Poke Plate', ['Poke Bowls', 'Healthy'], 4.5, '2K+', 25, 750, '🥗', 1, [
    ['Salmon Poke Bowl', 449, 0, '🍣', 'Bowls'], ['Paneer Poke Bowl', 299, 1, '🧀', 'Bowls'], ['Chicken Teriyaki Poke Bowl', 349, 0, '🍗', 'Bowls'], ['Seaweed Salad', 149, 1, '🥗', 'Sides'], ['Coconut Water', 79, 1, '🥥', 'Drinks']]],
  ['Catch of the Day', ['Seafood Grill', 'Seafood'], 4.6, '3K+', 39, 1500, '🦞', 1, [
    ['Grilled Lobster Tail', 799, 0, '🦞', 'Grills'], ['Butter Garlic Prawns', 449, 0, '🍤', 'Grills'], ['Grilled Pomfret', 499, 0, '🐟', 'Grills'], ['Fish Tikka', 329, 0, '🐟', 'Starters'], ['Lime Soda', 79, 1, '🍋', 'Drinks']]],
  ['Fish and Fries Co.', ['Fish and Chips', 'British'], 4.2, '2K+', 27, 600, '🍟', 0, [
    ['Crispy Fish and Chips', 329, 0, '🍟', 'Mains'], ['Fish Fingers', 199, 0, '🐟', 'Starters'], ['Veg Fritter Chips', 189, 1, '🥔', 'Mains'], ['Mushy Peas', 99, 1, '🫛', 'Sides'], ['Sticky Toffee Pudding', 159, 1, '🍮', 'Desserts']]],
  ['Shell House', ['Crab and Prawns', 'Seafood'], 4.5, '2K+', 41, 1400, '🦀', 1, [
    ['Butter Garlic Crab', 599, 0, '🦀', 'Mains'], ['Prawn Masala', 399, 0, '🍤', 'Mains'], ['Prawn Tempura', 349, 0, '🍤', 'Starters'], ['Crab Soup', 179, 0, '🍲', 'Soups'], ['Garlic Naan', 69, 1, '🫓', 'Breads']]],
  ['Souper Bowl', ['Soups', 'Healthy'], 4.2, '2K+', 20, 350, '🍲', 0, [
    ['Tomato Basil Soup', 129, 1, '🍅', 'Soups'], ['Chicken Hot and Sour Soup', 149, 0, '🍲', 'Soups'], ['Veg Manchow Soup', 139, 1, '🍜', 'Soups'], ['Cream of Mushroom Soup', 149, 1, '🍄', 'Soups'], ['Garlic Bread Sticks', 99, 1, '🥖', 'Breads']]],
  ['Thali Ghar', ['Thali', 'North Indian'], 4.5, '7K+', 28, 450, '🍱', 1, [
    ['Executive Veg Thali', 249, 1, '🍱', 'Thalis'], ['Special Non-Veg Thali', 329, 0, '🍱', 'Thalis'], ['Mini Thali', 179, 1, '🍱', 'Thalis'], ['Masala Papad', 49, 1, '🫓', 'Sides'], ['Gulab Jamun (2 pcs)', 69, 1, '🟤', 'Desserts']]],
  ['Kabul Kebab House', ['Afghani', 'Kebabs'], 4.4, '2K+', 35, 800, '🍢', 1, [
    ['Afghani Chicken Tikka', 289, 0, '🍗', 'Grills'], ['Kabuli Pulao', 299, 0, '🍚', 'Rice'], ['Afghani Paneer Tikka', 249, 1, '🧀', 'Grills'], ['Bolani', 119, 1, '🫓', 'Breads'], ['Dry Fruit Firni', 129, 1, '🍮', 'Desserts']]]
];

const EXTRA_RESTAURANTS = EXTRA_ROWS.map(([name, cuisines, rating, votes, eta, costForTwo, emoji, offers, dishes], i) => {
  const id = 'r' + (27 + i);
  const menu = dishes.map(([dish, price, veg, dishEmoji, cat], j) => ({
    id: id + '-' + (j + 1), name: dish, price, veg: veg === 1, emoji: dishEmoji, cat, best: j === 0
  }));
  return { id, name, cuisines, rating, votes, eta, costForTwo, pureVeg: menu.every(d => d.veg), emoji, offers: offers === 1, menu };
});

const EXTRA_CUISINES = EXTRA_RESTAURANTS.map(r => [r.cuisines[0], r.emoji]);
