type Props = {
  tabs: string[];
  active: string;
  onChange: (t: string) => void;
};

export default function Tabs({ tabs, active, onChange }: Props) {
  return (
    <div className="flex border-b border-r20">
      {tabs.map((t) => {
        const isActive = t === active;
        return (
          <div
            key={t}
            onClick={() => onChange(t)}
            className={`px-4 py-3.5 text-sm cursor-pointer border-b-2 ${
              isActive
                ? 'text-r100 border-r100 font-extrabold'
                : 'text-r40 border-transparent font-semibold hover:bg-r5 hover:text-r80'
            }`}
          >
            {t}
          </div>
        );
      })}
    </div>
  );
}
