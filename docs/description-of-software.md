---
title: "PackPilot: Description of the Software"
course: Applied Cloud Computing and Big Data
module: Module 5
---

# PackPilot: Description of the Software

PackPilot is a small application for generating a packing checklist for a trip and keeping track of it afterward. It targets a genuinely personal use case: someone who travels with family (in this case, with a child) and wants a sensible starting checklist rather than reconstructing one from memory every time, plus a simple way to check items off while packing and reuse or adjust past lists for the next trip.

The application presents a single web page. A user picks a climate or season (cold, mild, warm, tropical, or variable), a trip length in days, how many adults, children, and toddlers are traveling, and any relevant activities (beach, hiking, business, winter sports, or a formal event). Clicking "Generate checklist" produces a categorized list (clothing, toiletries, documents, electronics, and miscellaneous items), with quantities scaled to the trip length and the number and type of travelers, climate-appropriate additions (a winter coat for a cold climate, sunscreen and swimwear for a tropical one), activity-appropriate additions (hiking boots, a formal outfit, ski gloves), and child- or toddler-specific additions (diapers, a comfort toy, spare outfits for accidents) when a non-adult traveler is included. The user can then save the checklist under a trip name, check items off as they pack, add their own custom items, and later browse, search, or delete past trips.

Two design choices follow directly from this use case. First, the checklist generation is deliberately rule-based rather than based on an external destination lookup or an AI call: the user supplies the climate or season directly instead of naming a destination, which sidesteps the risk of a wrong guess about an unfamiliar place's weather and keeps the generation logic fully self-contained, with no external dependency and nothing to get wrong about a specific city. Second, the same item can legitimately be needed for more than one reason (swimwear for a tropical climate and for a beach activity, thermal layers for a cold climate and for winter sports); the generator is written so that these contributions are combined into one line with a summed quantity rather than appearing twice, which the architecture design document covers in more detail.

The application splits into two independently deployable microservices plus a database. In short: one service does nothing but turn a request into a checklist and holds no state of its own; the other serves the web page, asks the first service for a checklist draft on the user's behalf, and is the only component that reads from or writes to the database, where saved trips and their checklists are stored as MongoDB documents.

As with any single-user hobby tool, the scope here is modest: one family's trips, even planned several times a year, will never generate meaningful concurrent load. The benefits, challenges, and security log addresses this directly, along with what would have to be true in practice for the application's horizontal scaling to matter.
