/**
 * 
 */
package cs.uga.edu.dicgenerator;


/**
 * @author Mehdi
 *
 */
public class EntityFinder {

	static String text = " Fiat has completed its buyout of Chrysler, making the U.S. business a wholly-owned subsidiary of the Italian "
			+"carmaker as it gears up to use their combined resources to turn around its loss-making operations in Europe. The "
			+"company announced on January 1 that it had struck a $4.35 billion deal - cheaper than analysts had expected - to "
			+"gain full control of Chrysler, ending more than a year of tense talks that had obstructed Chief Executive Sergio "
			+"Marchionne's efforts to create the world's seventh-largest auto maker Marchionne said at the Detroit car show last "
			+"week that a listing of the combined entity was on the agenda for this year. While New York is the most liquid market, "
			+"Hong Kong is also an option, the CEO said, pledging to stay at the helm of the merged group for at least three years."
			+"The first big test for the merged Fiat-Chrysler will be a three-year industrial plan Marchionne is expected to unveil "
			+"in May, in which he will outline planned investments and models. Fiat has said its new strategy will focus on "
			+"revamping its Alfa Romeo brand and keeping production of the sporty marque in Italy as it seeks to utilize plants "
			+"operating below capacity, protect jobs and compete in the higher-margin premium segment of the market."
			+"Shares in Fiat were up 1.77 percent at 7.46 euros by 1630 GMT, outperforming a 0.11 percent rise for Milan's blue-chip index.";
	
	static String entities = "manage, production, world auto, compete, politicians, companies, u.s., models, discuss,"
			+ "skeptical, keeping, plants, use, fiat, needing, stake, unveil, finances, italy, ability,"
			+ "sensitive, debt, trust, gears, gain, combined, gmt, new, buyout, sporty, week, hong kong, "
			+ "resources, unions, percent, entity, marchionne, veba, seeks, deal, future, expand, losses, depends, "
			+ "chief executive, forced, milan, control, index, automatic, billion, eager, carmaker, shares, rising, "
			+ "cash, blue-chip, detroit car show, subsidiary, funded, uaw, struck, euros, line-up, marque, voluntary employee beneficiary association,"
			+ "equal, planned, headquarters, expected, meeting, listing, operations, premium segment, jobs, sergio marchionne,"
			+ "test, ties, auto maker, combine, focus, topic, million, rights issue, board, funds, announced, committed, "
			+ "closure, profit center, outside, home, europe, long-term, retiree, protect, outline, acquisition, paid, union, "
			+ "capacity, giving, completed, helm, product, chrysler, industrial, brand, italian, equity, investors, "
			+ "wholly-owned subsidiary, years ago, connection, alfa romeo, market, liquid, company, known, united auto workers, "
			+ "make it, rise, affiliated, healthcare, agenda, investments, annual, tense, strategy, potential, merger, ending, " 
			+ "plan, turn around, local, group, merged";
	public static void main(String[] args) {
		String [] tokens = entities.split(",");
		text = text.toLowerCase();
		for (String e : tokens) {
			int index = text.indexOf(e);
			if (index != -1) {
				text = text.replace(e, " * ");
			}
		}
		System.out.println("t: " + text);
	}

}
