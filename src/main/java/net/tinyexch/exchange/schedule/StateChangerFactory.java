package net.tinyexch.exchange.schedule;

import net.tinyexch.exchange.schedule.TradingFormInitializer;
import net.tinyexch.exchange.trading.form.StateChangeListener;
import net.tinyexch.exchange.trading.form.TradingModelStateChanger;
import net.tinyexch.exchange.trading.form.auction.Auction;
import net.tinyexch.exchange.trading.form.auction.AuctionProvider;
import net.tinyexch.exchange.trading.form.continuous.ContinuousTrading;
import net.tinyexch.exchange.trading.model.AuctionTradingModel;
import net.tinyexch.exchange.trading.model.ContinuousTradingInterruptedByAuctions;
import net.tinyexch.exchange.trading.model.TradingFormRunType;

import java.util.List;
import java.util.function.Supplier;

/**
 * Strategies to switch trading models through it's life cycle and trading forms.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-31
 */
public class StateChangerFactory {

    public static TradingFormInitializer<ContinuousTradingInterruptedByAuctions>
        createContinuousTrading_ContTrading(Supplier<ContinuousTrading> supplier, TradingFormRunType tradingFormRunType) {

        return new TradingFormInitializer<ContinuousTradingInterruptedByAuctions>(tradingFormRunType) {
            @Override
            public void setup(ContinuousTradingInterruptedByAuctions tradingModel, List<StateChangeListener> listeners) {
                tradingModel.initContinuousTrading(supplier, getTradingFormRuntype(), listeners);
            }
        };
    }


    public static TradingFormInitializer<ContinuousTradingInterruptedByAuctions>
        createAuction_ContTrading(Supplier<Auction> supplier, TradingFormRunType tradingFormRunType) {

        return new TradingFormInitializer<ContinuousTradingInterruptedByAuctions>(tradingFormRunType) {
            @Override
            public void setup(ContinuousTradingInterruptedByAuctions tradingModel, List<StateChangeListener> listeners) {
                tradingModel.initAuction(supplier, getTradingFormRuntype(), listeners);
            }
        };
    }


    public static TradingFormInitializer<AuctionTradingModel>
        createAuction_Auction(Supplier<Auction> supplier, TradingFormRunType tradingFormRunType) {

        return new TradingFormInitializer<AuctionTradingModel>(tradingFormRunType) {
            @Override
            public void setup(AuctionTradingModel tradingModel, List<StateChangeListener> listeners) {
                tradingModel.initAuction(supplier, getTradingFormRuntype(), listeners);
            }
        };
    }


    public static TradingModelStateChanger<AuctionProvider> startCall() {
        return model -> model.getAuction().startCallPhase();
    }

    public static TradingModelStateChanger<AuctionProvider> stopCall() {
        return model -> model.getAuction().stopCallPhase();
    }

    public static TradingModelStateChanger<AuctionProvider> determinePrice() {
        return model -> model.getAuction().determinePrice();
    }

    public static TradingModelStateChanger<AuctionProvider> balanceOrderbook() {
        return model -> model.getAuction().balanceOrderbook();
    }

    public static TradingModelStateChanger<ContinuousTradingInterruptedByAuctions> startContinuousTrading() {
        return model -> model.getContinuousTrading().start();
    }

    public static TradingModelStateChanger<ContinuousTradingInterruptedByAuctions> interruptWithAuction() {
        // TODO (FRa) : (FRa) : impl auction phases schedule
        return model -> model.getContinuousTrading().close();
    }

    /**
     * Stop everything no matter if continuous trading or an auction safeguard is running
     */
    public static TradingModelStateChanger<ContinuousTradingInterruptedByAuctions> stopContinuousTrading() {
        return model -> model.getContinuousTrading().close();
    }
}